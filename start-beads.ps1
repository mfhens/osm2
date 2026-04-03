# start-beads.ps1 — Start/stop/check the bd-managed Dolt server for this project.
# bd manages Dolt natively (no Docker required for single-project use).
#
# Usage:
#   .\start-beads.ps1              # Start Dolt if not already running
#   .\start-beads.ps1 -Stop        # Stop the Dolt server
#   .\start-beads.ps1 -Status      # Check server status

param(
    [switch]$Stop,
    [switch]$Status
)

Push-Location $PSScriptRoot

if ($Stop) {
    bd dolt stop
    exit $LASTEXITCODE
}

if ($Status) {
    bd dolt status
    exit $LASTEXITCODE
}

# Start if not already running
$statusOut = bd dolt status 2>&1
if ($statusOut -match "running") {
    Write-Host "Beads Dolt already running."
    bd dolt status
} else {
    bd dolt start
}

Pop-Location
