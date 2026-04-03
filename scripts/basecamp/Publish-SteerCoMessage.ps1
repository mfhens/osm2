# Builds a SteerCo-aligned Markdown digest from petitions/program-status.yaml and optionally posts it as a Basecamp Message.
#Requires -Version 5.1
param(
    [switch]$DryRun,
    [switch]$Draft,
    [switch]$NoSubscribe
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$Common = Join-Path $PSScriptRoot "Basecamp.Common.ps1"
. $Common

function Test-IsDonePetitionStatus {
    param([string]$Status)
    $s = $Status.ToLowerInvariant()
    return ($s -eq "implemented" -or $s -eq "validated")
}

function Get-CompletionMetrics {
    param($ProgramStatus)
    $petitions = @($ProgramStatus.petitions)
    $total = $petitions.Count
    $done = 0
    foreach ($p in $petitions) {
        if (Test-IsDonePetitionStatus $p.status) { $done++ }
    }
    $pct = if ($total -gt 0) { [math]::Round(100.0 * $done / $total) } else { 0 }
    [PSCustomObject]@{ Done = $done; Total = $total; CompletionPct = $pct }
}

function Build-SteerCoMarkdown {
    param($ProgramStatus)

    $prog = $ProgramStatus.program
    $asOf = $ProgramStatus.last_updated
    $metrics = Get-CompletionMetrics $ProgramStatus

    $lines = [System.Collections.Generic.List[string]]::new()
    $lines.Add("**Program:** $prog  ")
    $lines.Add("**As of:** $asOf  ")
    $lines.Add("")
    $lines.Add("## Executive summary")
    $lines.Add("- **Delivery:** $($metrics.Done) / $($metrics.Total) petitions complete ($($metrics.CompletionPct)%).")
    $openTb = @($ProgramStatus.technical_backlog | Where-Object { $_.status -ne "done" -and $_.status -ne "closed" })
    $lines.Add("- **Technical backlog (open):** $($openTb.Count) item(s).")
    if ($ProgramStatus.critical_path -and $ProgramStatus.critical_path.Count -gt 0) {
        $firstOpen = $null
        foreach ($cid in @($ProgramStatus.critical_path)) {
            $p = Get-PetitionById $ProgramStatus $cid
            if ($p -and -not (Test-IsDonePetitionStatus $p.status)) {
                $firstOpen = $cid
                break
            }
        }
        if ($firstOpen) {
            $lines.Add("- **Next on critical path:** $firstOpen (first not-done in ``critical_path``).")
        } else {
            $lines.Add("- **Critical path:** all items marked done in program-status.")
        }
    }
    $lines.Add("")
    $lines.Add("## Delivery progress (by phase)")
    if ($ProgramStatus.phases -and $ProgramStatus.phases.Count -gt 0) {
        foreach ($ph in @($ProgramStatus.phases)) {
            $lines.Add("")
            $lines.Add("### $($ph.name)")
            if ($ph.objective) {
                $lines.Add("*$($ph.objective)*")
                $lines.Add("")
            }
            $lines.Add("| Petition | Status |")
            $lines.Add("| --- | --- |")
            foreach ($ossId in @($ph.petitions)) {
                $p = Get-PetitionById $ProgramStatus $ossId
                $st = if ($p) { $p.status } else { "?" }
                $lines.Add("| $ossId | $st |")
            }
        }
    } else {
        $lines.Add("")
        $lines.Add("| Petition | Status |")
        $lines.Add("| --- | --- |")
        foreach ($p in @($ProgramStatus.petitions)) {
            $lines.Add("| $($p.id) | $($p.status) |")
        }
    }
    $lines.Add("")
    $lines.Add("## Critical path")
    if ($ProgramStatus.critical_path -and $ProgramStatus.critical_path.Count -gt 0) {
        foreach ($c in @($ProgramStatus.critical_path)) {
            $lines.Add("- $c")
        }
    } else {
        $lines.Add("- *(Set ``critical_path`` in program-status.yaml.)*")
    }
    $lines.Add("")
    $lines.Add("## In flight — next steps")
    $lines.Add("| Petition | Next step |")
    $lines.Add("| --- | --- |")
    $any = $false
    foreach ($p in @($ProgramStatus.petitions)) {
        if (Test-IsDonePetitionStatus $p.status) { continue }
        if (-not $p.next_step) { continue }
        $any = $true
        $step = [string]$p.next_step
        $step = $step.Replace("|", "\|")
        $lines.Add("| $($p.id) | $step |")
    }
    if (-not $any) {
        $lines.Add("| — | *(no ``next_step`` on open petitions)* |")
    }
    $lines.Add("")
    $lines.Add("## Risks, blockers, escalations")
    $blocked = @($ProgramStatus.petitions | Where-Object { $_.status -eq "blocked" })
    if ($blocked.Count -gt 0) {
        $lines.Add("**Blocked petitions:** " + ($blocked.id -join ", "))
    } else {
        $lines.Add("**Blocked petitions:** none recorded.")
    }
    $esc = $ProgramStatus.escalations
    if ($null -eq $esc) { $esc = @() }
    if ($esc.Count -eq 0) {
        $lines.Add("**Escalations:** none.")
    } else {
        $lines.Add("**Escalations:**")
        foreach ($e in @($esc)) {
            if ($e -is [string]) {
                $lines.Add("- $e")
            } elseif ($e.PSObject.Properties["summary"]) {
                $lines.Add("- $($e.summary)")
            } else {
                $lines.Add("- $($e | ConvertTo-Json -Compress -Depth 3)")
            }
        }
    }
    $lines.Add("")
    $lines.Add("## Architectural decisions")
    $lines.Add("Review recent changes under ``architecture/adr/`` (e.g. ``git log -n 5 --oneline architecture/adr``).")
    $lines.Add("")
    $lines.Add("## Technical backlog")
    if (@($ProgramStatus.technical_backlog).Count -eq 0) {
        $lines.Add("*(none)*")
    } else {
        $lines.Add("| ID | Title | Status |")
        $lines.Add("| --- | --- | --- |")
        foreach ($t in @($ProgramStatus.technical_backlog)) {
            $title = [string]$t.title
            $title = $title.Replace("|", "\|")
            $lines.Add("| $($t.id) | $title | $($t.status) |")
        }
    }
    $lines.Add("")
    $lines.Add("## Next actions (imperative)")
    $n = 0
    foreach ($p in @($ProgramStatus.petitions)) {
        if (Test-IsDonePetitionStatus $p.status) { continue }
        if ($p.next_step -and $n -lt 5) {
            $lines.Add("- **$($p.id):** $($p.next_step)")
            $n++
        }
    }
    foreach ($t in @($openTb)) {
        if ($n -ge 5) { break }
        $lines.Add("- **$($t.id):** $($t.title)")
        $n++
    }
    $lines.Add("")
    $lines.Add("---")
    $lines.Add("*Generated from ``petitions/program-status.yaml`` via ``scripts/basecamp/Publish-SteerCoMessage.ps1``.*")

    $lines -join "`n"
}

$repo = Get-Osm2RepoRoot
$data = Get-ProgramStatusObject $repo
$body = Build-SteerCoMarkdown $data
$title = "$($data.program) — SteerCo digest ($($data.last_updated))"

if ($DryRun) {
    Write-Host "TITLE: $title" -ForegroundColor Cyan
    Write-Host ""
    Write-Host $body
    exit 0
}

$config = Get-BasecampConfig $repo
$project = Resolve-BasecampProject $config

$msgArgs = @("message", $title, $body, "--in", $project)
if ($Draft) { $msgArgs += "--draft" }
if ($NoSubscribe) { $msgArgs += "--no-subscribe" }

Invoke-Basecamp $msgArgs
Write-Host "Posted SteerCo message: $title" -ForegroundColor Green
