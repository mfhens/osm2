#!/usr/bin/env python3
"""Emit petitions/program-status.yaml as JSON on stdout (for PowerShell automation)."""

from __future__ import annotations

import json
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
PROGRAM_STATUS = REPO_ROOT / "petitions" / "program-status.yaml"

try:
    import yaml  # type: ignore
except ImportError:
    import subprocess

    subprocess.run([sys.executable, "-m", "pip", "install", "pyyaml", "-q"], check=True)
    import yaml  # type: ignore


def main() -> None:
    if not PROGRAM_STATUS.exists():
        print(f"ERROR: {PROGRAM_STATUS} not found", file=sys.stderr)
        sys.exit(1)
    data = yaml.safe_load(PROGRAM_STATUS.read_text(encoding="utf-8"))
    json.dump(data, sys.stdout, ensure_ascii=False, indent=2)
    sys.stdout.write("\n")


if __name__ == "__main__":
    main()
