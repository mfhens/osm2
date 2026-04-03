# Pushes Beads issue comments to a Basecamp card (optional; requires card recording ID mapping).
#Requires -Version 5.1
param(
    [Parameter(Mandatory = $true)]
    [string]$IssueId,
    [string]$PetitionId,
    [string]$CardRecordingId,
    [string]$Pattern = "LEARNED|PATTERN|FACT|INVESTIGATION",
    [switch]$DryRun,
    [switch]$AllComments
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$Common = Join-Path $PSScriptRoot "Basecamp.Common.ps1"
. $Common

$repo = Get-Osm2RepoRoot
$config = Get-BasecampConfig $repo
$project = Resolve-BasecampProject $config

$cardId = $CardRecordingId
if (-not $cardId -and $PetitionId) {
    $mapPath = Join-Path $repo "scripts\basecamp\card-map.json"
    if (-not (Test-Path $mapPath)) {
        throw "Provide -CardRecordingId or copy scripts/basecamp/card-map.example.json to card-map.json with a mapping for $PetitionId."
    }
    $map = Get-Content -LiteralPath $mapPath -Encoding UTF8 | ConvertFrom-Json -Depth 10
    $cardId = [string]$map.$PetitionId
    if (-not $cardId) {
        throw "card-map.json has no entry for petition $PetitionId."
    }
}

if (-not $cardId) { throw "Set -CardRecordingId or -PetitionId with card-map.json." }

$bdOut = & bd "comments" $IssueId "--json" 2>&1
if ($LASTEXITCODE -ne 0) { throw "bd comments failed: $bdOut" }
$raw = $bdOut | Out-String
$parsed = $raw | ConvertFrom-Json -Depth 20
$comments = @()
if ($parsed.PSObject.Properties["data"]) {
    $comments = @($parsed.data)
} elseif ($parsed -is [array]) {
    $comments = @($parsed)
} else {
    $comments = @($parsed)
}

$rx = if ($AllComments) { $null } else { [regex]$Pattern }
$posted = 0
foreach ($c in $comments) {
    $body = $null
    if ($c.PSObject.Properties["body"]) { $body = [string]$c.body }
    elseif ($c.PSObject.Properties["content"]) { $body = [string]$c.content }
    elseif ($c.PSObject.Properties["text"]) { $body = [string]$c.text }
    if (-not $body) { continue }
    if ($rx -and -not $rx.IsMatch($body)) { continue }

    $snippet = $body
    if ($snippet.Length -gt 3500) { $snippet = $snippet.Substring(0, 3497) + "..." }
    $block = "**Beads ($IssueId)**`n`n$snippet"

    if ($DryRun) {
        Write-Host "[dry-run] comment on card $cardId ($($snippet.Length) chars)" -ForegroundColor Yellow
        $posted++
        continue
    }
    Invoke-Basecamp @("comment", $cardId, $block, "--in", $project)
    $posted++
    Write-Host "Posted comment $posted" -ForegroundColor Green
}

if ($posted -eq 0) {
    Write-Host "No matching comments to post (use -AllComments or adjust -Pattern)." -ForegroundColor DarkYellow
}
