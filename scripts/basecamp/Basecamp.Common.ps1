# Shared helpers for scripts/basecamp/*.ps1
# Supports osm2-style program-status (flat: program string, petitions array) and
# opendebt-style (nested program object, petitions as a mapping).
#Requires -Version 5.1

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# Native command output (e.g. python → JSON) must be decoded as UTF-8 on Windows; otherwise
# YAML/em dash/Danish letters from program-status.yaml become mojibake in Basecamp.
function Use-ConsoleUtf8 {
    param([ScriptBlock]$Script)
    $utf8 = [System.Text.UTF8Encoding]::new($false)
    $prevOut = [Console]::OutputEncoding
    try {
        [Console]::OutputEncoding = $utf8
        & $Script
    } finally {
        [Console]::OutputEncoding = $prevOut
    }
}

function Get-RepoRoot {
    $here = $PSScriptRoot
    while ($here) {
        $candidate = Join-Path $here "petitions\program-status.yaml"
        if (Test-Path $candidate) { return $here }
        $parent = Split-Path $here -Parent
        if ($parent -eq $here) { break }
        $here = $parent
    }
    throw "Could not locate repo root (petitions/program-status.yaml not found)."
}

function Get-ProgramStatusObject {
    param([string]$RepoRoot)
    $py = Join-Path $PSScriptRoot "read_program_status.py"
    if (-not (Test-Path $py)) { throw "Missing $py" }
    $json = Use-ConsoleUtf8 {
        $env:PYTHONUTF8 = "1"
        & python $py 2>$null
    }
    if ($LASTEXITCODE -ne 0) { throw "read_program_status.py failed: $json" }
    $json | ConvertFrom-Json -Depth 50
}

<#
.SYNOPSIS
  Normalizes program-status.yaml (osm2 or opendebt shape) to a single PSCustomObject for scripts.
#>
function Get-ProgramView {
    param($Raw)
    if ($null -eq $Raw) { throw "program-status data is null." }

    # osm2: program: osm2  (scalar), petitions: [ { id: OSS-01, ... }, ... ]
    if ($Raw.program -is [string]) {
        return [PSCustomObject]@{
            ProgramLabel     = [string]$Raw.program
            LastUpdated      = [string]$Raw.last_updated
            Phases           = @($Raw.phases)
            CriticalPath     = @($Raw.critical_path)
            Petitions        = @($Raw.petitions)
            TechnicalBacklog = @($Raw.technical_backlog)
            Escalations      = @($Raw.escalations)
        }
    }

    # opendebt: program: { name, updated_at, petitions: { petition001: { ... } }, ... }
    $pr = $Raw.program
    if ($null -eq $pr) { throw "program-status.yaml missing 'program' section." }

    $petitions = [System.Collections.Generic.List[object]]::new()
    $pdict = $pr.petitions
    $pprops = @($pdict.PSObject.Properties)
    if ($null -ne $pdict -and $pprops.Count -gt 0) {
        foreach ($prop in $pprops) {
            $id = $prop.Name
            $val = $prop.Value
            $row = [ordered]@{ id = $id }
            foreach ($p in $val.PSObject.Properties) {
                if ($p.Name -eq "id") { continue }
                $row[$p.Name] = $p.Value
            }
            $petitions.Add([PSCustomObject]$row)
        }
    }

    $esc = @()
    if ($null -ne $Raw.escalations) { $esc = @($Raw.escalations) }

    return [PSCustomObject]@{
        ProgramLabel     = [string]$pr.name
        LastUpdated      = [string]$pr.updated_at
        Phases           = @($pr.phases)
        CriticalPath     = @($pr.critical_path)
        Petitions        = @($petitions)
        TechnicalBacklog = @($pr.technical_backlog)
        Escalations      = $esc
    }
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
    Use-ConsoleUtf8 {
        & basecamp @Arguments
    }
    if ($LASTEXITCODE -ne 0) {
        throw "basecamp failed ($LASTEXITCODE): basecamp $($Arguments -join ' ')"
    }
}

function Get-BasecampJsonData {
    param([string[]]$BcArgs)
    $out = Use-ConsoleUtf8 {
        & basecamp @BcArgs 2>&1
    }
    if ($LASTEXITCODE -ne 0) { throw "basecamp failed: basecamp $($BcArgs -join ' ') — $out" }
    $o = $out | Out-String
    $parsed = $o | ConvertFrom-Json -Depth 50
    $dataProp = $parsed.PSObject.Properties["data"]
    if ($null -ne $dataProp -and $null -ne $dataProp.Value) {
        return @($dataProp.Value)
    }
    return @()
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
    param($View, [string]$Id)
    foreach ($p in $View.Petitions) {
        if ([string]$p.id -eq $Id) { return $p }
    }
    return $null
}

function Test-TbIsClosed {
    param([string]$Status)
    if (-not $Status) { return $false }
    $s = $Status.ToLowerInvariant()
    return ($s -eq "done" -or $s -eq "closed" -or $s -eq "implemented" -or $s -eq "wont_fix" -or $s -eq "superseded")
}
