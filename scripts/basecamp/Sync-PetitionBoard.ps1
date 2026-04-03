# Creates or updates Kanban cards for petitions and moves them to columns by status (see .basecamp/config.example.json).
#Requires -Version 5.1
param(
    [switch]$DryRun
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$Common = Join-Path $PSScriptRoot "Basecamp.Common.ps1"
. $Common

function Find-CardForPetition {
    param($Cards, [string]$PetitionId)
    foreach ($c in $Cards) {
        if ($null -eq $c) { continue }
        $t = [string]$c.title
        if ($t.TrimStart().StartsWith($PetitionId)) { return $c }
    }
    return $null
}

function Get-TargetColumnId {
    param($Config, [string]$Status)
    $cols = $Config.columns
    if (-not $cols) { throw "config.json missing ""columns"" map." }
    $key = $Status.ToLowerInvariant()
    $prop = $cols.PSObject.Properties[$key]
    $cid = if ($null -ne $prop) { [string]$prop.Value } else { "" }
    if ([string]::IsNullOrWhiteSpace($cid)) {
        $defProp = $cols.PSObject.Properties["default"]
        if ($null -ne $defProp -and -not [string]::IsNullOrWhiteSpace([string]$defProp.Value)) {
            return [string]$defProp.Value
        }
        throw "No column mapping for status '$Status' (and no columns.default)."
    }
    return $cid
}

function Escape-Html {
    param([string]$Text)
    if (-not $Text) { return "" }
    $t = $Text
    $t = $t.Replace("&", "&amp;").Replace("<", "&lt;").Replace(">", "&gt;")
    return $t
}

$repo = Get-RepoRoot
$raw = Get-ProgramStatusObject $repo
$view = Get-ProgramView $raw
$config = Get-BasecampConfig $repo
$project = Resolve-BasecampProject $config
$cardTable = [string]$config.card_table_id
if (-not $cardTable) { throw "Set card_table_id in .basecamp/config.json (basecamp cards columns --in <project>)." }

$listArgs = @("cards", "list", "--in", $project, "--json", "--card-table", $cardTable)
$existing = Get-BasecampJsonData $listArgs
if ($DryRun) {
    Write-Host "[dry-run] Read-only card list loaded ($($existing.Count) cards). No create/move will run." -ForegroundColor DarkGray
}

foreach ($p in @($view.Petitions)) {
    $ossId = [string]$p.id
    $title = "$ossId — $($p.title)"
    $col = Get-TargetColumnId $config $p.status
    $bodyHtml = "<p><strong>$ossId</strong> — $(Escape-Html $p.title)</p><p>Status: $(Escape-Html $p.status)</p>"

    $card = Find-CardForPetition $existing $ossId

    if (-not $card) {
        if ($DryRun) {
            Write-Host "[dry-run] create card '$title' -> column $col" -ForegroundColor Yellow
        } else {
            Invoke-Basecamp @("card", $title, $bodyHtml, "--in", $project, "--column", $col, "--card-table", $cardTable)
            Write-Host "Created card $ossId" -ForegroundColor Green
            $existing = Get-BasecampJsonData $listArgs
        }
        continue
    }

    $cur = Get-CardColumnId $card
    if ($cur -eq $col) {
        Write-Host "$ossId already in column $col" -ForegroundColor DarkGray
        continue
    }
    if ($DryRun) {
        Write-Host "[dry-run] move card $($card.id) ($ossId) from $cur -> $col" -ForegroundColor Yellow
    } else {
        Invoke-Basecamp @("cards", "move", [string]$card.id, "--to", $col, "--in", $project, "--card-table", $cardTable)
        Write-Host "Moved $ossId to column $col" -ForegroundColor Green
    }
}
