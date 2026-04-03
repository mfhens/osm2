# Shared helpers for scripts/basecamp/*.ps1
#Requires -Version 5.1

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-Osm2RepoRoot {
    $here = $PSScriptRoot
    while ($here) {
        $candidate = Join-Path $here "petitions\program-status.yaml"
        if (Test-Path $candidate) { return $here }
        $parent = Split-Path $here -Parent
        if ($parent -eq $here) { break }
        $here = $parent
    }
    throw "Could not locate osm2 repo root (petitions/program-status.yaml not found)."
}

function Get-ProgramStatusObject {
    param([string]$RepoRoot)
    $py = Join-Path $PSScriptRoot "read_program_status.py"
    if (-not (Test-Path $py)) { throw "Missing $py" }
    $json = & python $py 2>&1
    if ($LASTEXITCODE -ne 0) { throw "read_program_status.py failed: $json" }
    $json | ConvertFrom-Json -Depth 50
}

function Get-BasecampConfig {
    param([string]$RepoRoot)
    $p = Join-Path $RepoRoot ".basecamp\config.json"
    if (-not (Test-Path $p)) {
        throw "Missing $p — copy .basecamp/config.example.json to .basecamp/config.json and run: basecamp config trust"
    }
    Get-Content -LiteralPath $p -Encoding UTF8 | ConvertFrom-Json -Depth 20
}

function Resolve-BasecampProject {
    param($Config)
    if ($env:BASECAMP_PROJECT) { return $env:BASECAMP_PROJECT }
    if ($Config.project_id) { return [string]$Config.project_id }
    throw "Set project_id in .basecamp/config.json or environment variable BASECAMP_PROJECT."
}

function Invoke-Basecamp {
    param([string[]]$Arguments)
    & basecamp @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "basecamp failed ($LASTEXITCODE): basecamp $($Arguments -join ' ')"
    }
}

function Get-CardColumnId {
    param($Card)
    if ($null -eq $Card) { return $null }
    if ($Card.PSObject.Properties["parent"] -and $Card.parent -and $Card.parent.id) {
        return [string]$Card.parent.id
    }
    if ($Card.PSObject.Properties["column_id"] -and $Card.column_id) {
        return [string]$Card.column_id
    }
    return $null
}

function Get-PetitionById {
    param($ProgramStatus, [string]$Id)
    foreach ($p in $ProgramStatus.petitions) {
        if ($p.id -eq $Id) { return $p }
    }
    return $null
}
