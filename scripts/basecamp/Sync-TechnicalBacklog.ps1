# Ensures technical_backlog items from program-status.yaml exist as Basecamp todos (and completes them when status is done).
#Requires -Version 5.1
param(
    [switch]$DryRun
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$Common = Join-Path $PSScriptRoot "Basecamp.Common.ps1"
. $Common

function Get-BasecampJsonData {
    param([string[]]$BcArgs)
    $out = & basecamp @BcArgs 2>&1
    if ($LASTEXITCODE -ne 0) { throw "basecamp failed: basecamp $($BcArgs -join ' ') — $out" }
    $o = $out | Out-String
    $parsed = $o | ConvertFrom-Json -Depth 50
    $dataProp = $parsed.PSObject.Properties["data"]
    if ($null -ne $dataProp -and $null -ne $dataProp.Value) {
        return @($dataProp.Value)
    }
    return @()
}

function Find-TodoForTb {
    param($Todos, [string]$TbId)
    $pattern = "^\s*" + [regex]::Escape($TbId) + "\b"
    foreach ($t in $Todos) {
        $title = [string]$t.title
        if ($title -match $pattern) { return $t }
    }
    return $null
}

function Get-AllTodos {
    param([string]$Project)
    $inc = Get-BasecampJsonData @("todos", "list", "--in", $Project, "--json", "--all", "-s", "incomplete")
    $done = Get-BasecampJsonData @("todos", "list", "--in", $Project, "--json", "--all", "-s", "completed")
    $byId = @{}
    foreach ($t in @($inc) + @($done)) {
        $byId[[string]$t.id] = $t
    }
    @($byId.Values)
}

$repo = Get-RepoRoot
$raw = Get-ProgramStatusObject $repo
$view = Get-ProgramView $raw
$config = Get-BasecampConfig $repo
$project = Resolve-BasecampProject $config
$listId = [string]$config.todolist_id
if (-not $listId) { throw "Set todolist_id in .basecamp/config.json (basecamp todolists list --in <project>)." }

$todos = Get-AllTodos $project

foreach ($tb in @($view.TechnicalBacklog)) {
    $tid = [string]$tb.id
    $line = "$tid — $($tb.title)"
    $todo = Find-TodoForTb $todos $tid

    if (-not $todo) {
        if ($DryRun) {
            Write-Host "[dry-run] create todo '$line'" -ForegroundColor Yellow
        } else {
            Invoke-Basecamp @("todo", $line, "--in", $project, "--list", $listId)
            Write-Host "Created todo $tid" -ForegroundColor Green
            $todos = Get-AllTodos $project
            $todo = Find-TodoForTb $todos $tid
        }
        if ($DryRun) { continue }
    }

    if (-not $todo) { continue }

    $wantDone = Test-TbIsClosed $tb.status
    $isDone = $false
    if ($todo.PSObject.Properties["completed"]) { $isDone = [bool]$todo.completed }
    elseif ($todo.PSObject.Properties["status"]) { $isDone = ($todo.status -eq "completed") }

    if ($wantDone -and -not $isDone) {
        if ($DryRun) {
            Write-Host "[dry-run] complete todo $($todo.id) ($tid)" -ForegroundColor Yellow
        } else {
            Invoke-Basecamp @("done", [string]$todo.id)
            Write-Host "Completed todo $tid" -ForegroundColor Green
        }
    } elseif (-not $wantDone -and $isDone) {
        Write-Host "Note: $tid is open in YAML but todo $($todo.id) is completed — reopen manually if needed." -ForegroundColor DarkYellow
    } else {
        Write-Host "$tid synced (status: $($tb.status))" -ForegroundColor DarkGray
    }
}
