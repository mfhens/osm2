#!/usr/bin/env bash
# start-beads.sh — Start/stop/check the bd-managed Dolt server for this project.
# bd manages Dolt natively (no Docker required for single-project use).
#
# Usage:
#   ./start-beads.sh              # Start Dolt if not already running
#   ./start-beads.sh --stop       # Stop the Dolt server
#   ./start-beads.sh --status     # Check server status

set -euo pipefail
cd "$(dirname "$0")"

case "${1:-start}" in
  --stop|-s)
    bd dolt stop
    ;;
  --status|-S)
    bd dolt status
    ;;
  start|"")
    if bd dolt status 2>&1 | grep -q "running"; then
      echo "Beads Dolt already running."
      bd dolt status
    else
      bd dolt start
    fi
    ;;
  *)
    echo "Usage: $0 [--stop | --status]"
    exit 1
    ;;
esac
