#!/usr/bin/env python3
"""Emit petitions/program-status.yaml as JSON on stdout (for PowerShell automation)."""

from __future__ import annotations

import datetime
import json
import sys
from decimal import Decimal
from pathlib import Path


def _json_default(obj: object):
    if isinstance(obj, (datetime.datetime, datetime.date)):
        return obj.isoformat()
    if isinstance(obj, Decimal):
        return float(obj)
    raise TypeError(f"Object of type {type(obj).__name__} is not JSON serializable")

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
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass
    data = yaml.safe_load(PROGRAM_STATUS.read_text(encoding="utf-8"))
    text = json.dumps(data, ensure_ascii=False, indent=2, default=_json_default) + "\n"
    sys.stdout.buffer.write(text.encode("utf-8"))


if __name__ == "__main__":
    main()
