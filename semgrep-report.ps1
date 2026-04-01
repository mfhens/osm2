#!/usr/bin/env pwsh
#Requires -Version 7
<#
.SYNOPSIS
    Runs semgrep via Docker and produces a filtered report.
.DESCRIPTION
    Executes semgrep with p/java, p/secrets, and the local magic-strings rule.
    Rulesets are auto-refreshed from semgrep.dev if older than 7 days.
    Magic-string findings are post-filtered: only string values that appear
    in 3 or more distinct findings are reported as violations.

    Output: console summary + reports/semgrep-report.json (full)
                             + reports/semgrep-magic-strings.txt (filtered)
.PARAMETER RulesOnly
    Skip p/java and p/secrets; run only the local magic-strings rule.
#>
param(
    [switch]$RulesOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$RepoRoot  = $PSScriptRoot
$ReportDir = Join-Path $RepoRoot "reports"
$RulesDir  = Join-Path $RepoRoot "config\semgrep"
New-Item -ItemType Directory -Force -Path $ReportDir | Out-Null

$FullReportPath  = Join-Path $ReportDir "semgrep-report.json"
$MagicReportPath = Join-Path $ReportDir "semgrep-magic-strings.txt"

# Auto-refresh downloaded rulesets if older than 7 days
if (-not $RulesOnly) {
    $maxAge  = [TimeSpan]::FromDays(7)
    $ruleMap = [ordered]@{
        "p-java.yaml"    = "https://semgrep.dev/c/p/java"
        "p-secrets.yaml" = "https://semgrep.dev/c/p/secrets"
    }
    foreach ($fileName in $ruleMap.Keys) {
        $file  = Join-Path $RulesDir $fileName
        $stale = (-not (Test-Path $file)) -or
                 ((Get-Date) - (Get-Item $file).LastWriteTime) -gt $maxAge
        if ($stale) {
            Write-Host "Refreshing $fileName ..." -ForegroundColor Cyan
            try {
                Invoke-WebRequest -Uri $ruleMap[$fileName] -OutFile $file -UseBasicParsing
                Write-Host "  OK ($([Math]::Round((Get-Item $file).Length / 1KB)) KB)" -ForegroundColor Green
            } catch {
                if (Test-Path $file) {
                    Write-Warning "Refresh failed -- using cached $fileName : $_"
                } else {
                    throw "Cannot download $fileName and no cache exists: $_"
                }
            }
        } else {
            $age = [Math]::Round(((Get-Date) - (Get-Item $file).LastWriteTime).TotalDays, 1)
            Write-Host "  $fileName is up to date (${age}d old)" -ForegroundColor DarkGray
        }
    }
}

# Build config args
$configs = @("--config", "/src/config/semgrep/magic-strings.yaml")
if (-not $RulesOnly) {
    $configs += "--config", "/src/config/semgrep/p-java.yaml"
    $configs += "--config", "/src/config/semgrep/p-secrets.yaml"
}

# Run semgrep via Docker -- use --output so JSON goes directly to file (avoids
# stdout/stderr interleaving that breaks JSON capture with 2>&1).
Write-Host "Running semgrep (Docker)..." -ForegroundColor Cyan
$dockerArgs = @(
    "run", "--rm",
    "-v", "${RepoRoot}:/src",
    "--workdir", "/src",
    "semgrep/semgrep",
    "semgrep"
) + $configs + @(
    "--json",
    "--output", "/src/reports/semgrep-report.json",
    "--metrics=off",
    "--exclude", "target",
    "--exclude", "*.class",
    "--exclude", "config/semgrep",
    "--exclude", "reports",
    "/src"
)

docker @dockerArgs
if ($LASTEXITCODE -ge 2) {
    Write-Host "semgrep exited with error code $LASTEXITCODE" -ForegroundColor Red
    exit $LASTEXITCODE
}

if (-not (Test-Path $FullReportPath)) {
    Write-Host "semgrep did not produce output file: $FullReportPath" -ForegroundColor Red
    exit 1
}

$report = Get-Content $FullReportPath -Raw | ConvertFrom-Json
$total  = $report.results.Count
$errors = $report.errors.Count
Write-Host "Semgrep complete: $total findings, $errors errors" -ForegroundColor Green

# Separate magic-string findings
$magicIds = @("magic-string-assignment", "magic-string-comparison", "magic-string-return")
$magicFindings = @($report.results | Where-Object { $magicIds -contains ($_.check_id -split '\.')[-1] })
$otherFindings = @($report.results | Where-Object { $magicIds -notcontains ($_.check_id -split '\.')[-1] })

$hits = @($magicFindings | ForEach-Object {
    $lit = try { $_.extra.metavars.'$LITERAL'.abstract_content } catch { $null }
    if (-not $lit -and $_.extra.message -match '"([^"]+)"') { $lit = "`"$($matches[1])`"" }
    [PSCustomObject]@{
        Literal = $lit
        File    = $_.path -replace [regex]::Escape($RepoRoot + "\"), ""
        Line    = $_.start.line
        Rule    = ($_.check_id -split '\.')[-1]
    }
})

$groups = @($hits | Group-Object Literal | Where-Object { $_.Count -ge 3 } | Sort-Object Count -Descending)

$lines  = @()
$lines += "# Magic String Candidates (3+ occurrences)"
$lines += "# Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm')"
$lines += ""
foreach ($g in $groups) {
    $lines += "## `"$($g.Name)`"  ($($g.Count) occurrences)"
    foreach ($item in $g.Group | Sort-Object File, Line) { $lines += "  $($item.File):$($item.Line)" }
    $lines += ""
}
if ($groups.Count -eq 0) { $lines += "(no string literals reach the 3-occurrence threshold)" }
$lines | Set-Content $MagicReportPath

Write-Host ""
Write-Host "Magic string candidates (3+ occurrences): $($groups.Count)" -ForegroundColor Yellow
$groups | ForEach-Object { Write-Host "  `"$($_.Name)`"  -- $($_.Count)x" }

if ($otherFindings.Count -gt 0) {
    Write-Host ""
    Write-Host "Other findings by severity:" -ForegroundColor Cyan
    $otherFindings | Group-Object { $_.extra.severity } | Sort-Object Name |
        ForEach-Object { Write-Host "  $($_.Name): $($_.Count)" }
    Write-Host ""
    $otherFindings | Select-Object -First 20 | ForEach-Object {
        $file = $_.path -replace [regex]::Escape($RepoRoot + "\"), ""
        Write-Host "  [$($_.extra.severity)] $(($_.check_id -split '\.')[-1])"
        Write-Host "    ${file}:$($_.start.line)"
    }
}

Write-Host ""
Write-Host "Full report:   $FullReportPath"
Write-Host "Magic strings: $MagicReportPath"