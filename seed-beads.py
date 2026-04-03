#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
seed-beads.py — Seed Beads from petitions/program-status.yaml (osm2).

Creates:
  • Program umbrella epic (tracks petitions/program-status.yaml)
  • One epic per **implemented** petition, closed with history comments (importable audit trail)
  • Open technical backlog items as tasks

Prerequisites:
  • `bd init` has been run in this repo (Windows: use `bd init -p osm2 --skip-agents --server`)
  • `bd` on PATH; Dolt server reachable (see start-beads.ps1)

Usage:
  python seed-beads.py              # Seed program + implemented history + open TB
  python seed-beads.py --dry-run     # Print planned bd commands only
"""

from __future__ import annotations

import argparse
import os
import subprocess
import sys
import tempfile
from pathlib import Path

if sys.stdout.encoding is None or sys.stdout.encoding.lower() != "utf-8":
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

try:
    import yaml  # type: ignore
except ImportError:
    subprocess.run([sys.executable, "-m", "pip", "install", "pyyaml", "-q"], check=True)
    import yaml  # type: ignore

REPO_ROOT = Path(__file__).resolve().parent
PROGRAM_STATUS = REPO_ROOT / "petitions" / "program-status.yaml"

SKIP_TB = frozenset({"done", "closed", "implemented", "wont_fix", "superseded"})


def run_bd(args: list[str], dry_run: bool) -> str:
    cmd = ["bd", *args]
    s = " ".join(cmd)
    if dry_run:
        print(f"  [dry-run] {s}")
        return ""
    r = subprocess.run(
        cmd,
        cwd=str(REPO_ROOT),
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    out = (r.stdout or "").strip()
    if r.returncode != 0:
        err = (r.stderr or r.stdout or "").strip()
        raise RuntimeError(f"bd failed ({r.returncode}): {s}\n{err}")
    return out


def create_issue(title: str, issue_type: str, extra: list[str], dry_run: bool) -> str:
    args = ["create", "--title", title, "--type", issue_type, "--silent", *extra]
    out = run_bd(args, dry_run)
    if dry_run:
        return "dry-run-id"
    line = out.strip().splitlines()[-1] if out else ""
    # --silent prints single id
    return line.strip() or out.strip()


def write_body_file(text: str) -> Path:
    fd, name = tempfile.mkstemp(prefix="beads-body-", suffix=".md", text=True)
    os.close(fd)
    p = Path(name)
    p.write_text(text, encoding="utf-8")
    return p


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args()
    dry = args.dry_run

    if not PROGRAM_STATUS.exists():
        print(f"ERROR: {PROGRAM_STATUS} not found", file=sys.stderr)
        sys.exit(1)

    data = yaml.safe_load(PROGRAM_STATUS.read_text(encoding="utf-8"))
    petitions = data.get("petitions") or []
    if not isinstance(petitions, list):
        print("ERROR: expected petitions: as a YAML list", file=sys.stderr)
        sys.exit(1)

    technical_backlog = data.get("technical_backlog") or []
    open_tb_items = [
        tb
        for tb in technical_backlog
        if tb.get("id")
        and str(tb.get("status", "open")).lower() not in SKIP_TB
    ]

    print(f"Using {PROGRAM_STATUS}\n")

    # --- Program umbrella ---
    prog_title = "osm2 — petition program (petitions/program-status.yaml)"
    prog_body = (
        f"Umbrella epic for **osm2** delivery tracking.\n\n"
        f"Canonical backlog: `{PROGRAM_STATUS.relative_to(REPO_ROOT).as_posix()}`\n"
        f"Program id: `{data.get('program')}` · last_updated: `{data.get('last_updated')}`\n"
    )
    body_path = write_body_file(prog_body)
    try:
        prog_id = create_issue(
            prog_title,
            "epic",
            ["-p", "2", "--body-file", str(body_path), "-l", "program,osm2"],
            dry,
        )
    finally:
        body_path.unlink(missing_ok=True)

    print(f"Program epic: {prog_id}")

    # --- Implemented petitions (history) ---
    implemented = [p for p in petitions if str(p.get("status", "")).lower() == "implemented"]
    implemented.sort(key=lambda x: str(x.get("id", "")))

    for p in implemented:
        pid = p.get("id", "?")
        ptitle = p.get("title", "")
        title = f"{pid}: {ptitle}"

        hist_lines = [
            "## Import snapshot (program-status.yaml)",
            "",
            f"- **status**: {p.get('status')}",
            f"- **created**: {p.get('created', '')}",
            f"- **implemented**: {p.get('implemented', '')}",
            f"- **commit**: `{p.get('commit', '')}`",
            f"- **gherkin_scenarios**: {p.get('gherkin_scenarios', '')}",
            f"- **functional_requirements**: {p.get('functional_requirements', '')}",
            "",
            "### Legal basis",
        ]
        for lb in p.get("legal_basis") or []:
            hist_lines.append(f"- {lb}")
        arts = p.get("artifacts") or {}
        hist_lines.extend(["", "### Artifacts", f"- petition: `{arts.get('petition', '')}`"])
        hist_lines.append(f"- outcome_contract: `{arts.get('outcome_contract', '')}`")
        hist_lines.append(f"- feature_file: `{arts.get('feature_file', '')}`")
        if arts.get("test_features"):
            hist_lines.append("- test_features:")
            for tf in arts["test_features"]:
                hist_lines.append(f"  - `{tf}`")
        cf = p.get("catala_files")
        if cf:
            hist_lines.extend(["", "### Catala", *[f"- `{x}`" for x in cf]])
        notes = (p.get("notes") or "").strip()
        if notes:
            hist_lines.extend(["", "### Notes (from YAML)", notes])

        description = "\n".join(hist_lines)
        desc_path = write_body_file(description)
        try:
            eid = create_issue(
                title,
                "epic",
                [
                    "-p",
                    "2",
                    "--parent",
                    prog_id,
                    "--body-file",
                    str(desc_path),
                    "-l",
                    "petition,implemented",
                ],
                dry,
            )
        finally:
            desc_path.unlink(missing_ok=True)

        print(f"  {pid} epic: {eid}")

        history_comment = (
            "AUDIT / HISTORY — imported from petitions/program-status.yaml\n\n"
            + description
            + "\n\n"
            "This issue is **closed** to reflect delivered work; use Dolt history / `bd history` for edits."
        )
        if not dry:
            cpath = write_body_file(history_comment)
            try:
                run_bd(["comment", eid, "--file", str(cpath)], dry_run=False)
            finally:
                cpath.unlink(missing_ok=True)

        if not dry:
            run_bd(
                [
                    "close",
                    eid,
                    "--reason",
                    "Implemented (seeded from program-status.yaml — delivery complete).",
                ],
                dry_run=False,
            )

    # --- Technical backlog (open) ---
    for tb in technical_backlog:
        tid = tb.get("id", "")
        st = str(tb.get("status", "open")).lower()
        if st in SKIP_TB or not tid:
            continue
        title = f"{tid}: {tb.get('title', tid)}"
        pri = tb.get("priority", "medium")
        pmap = {"critical": "0", "high": "1", "medium": "2", "low": "3"}
        pstr = pmap.get(str(pri).lower(), "2")
        tdesc = (tb.get("description") or "").strip()
        tpath = write_body_file(tdesc + "\n\n_Source: petitions/program-status.yaml technical_backlog_\n")
        try:
            bid = create_issue(
                title,
                "task",
                ["-p", pstr, "--parent", prog_id, "--body-file", str(tpath), "-l", "technical-backlog"],
                dry,
            )
        finally:
            tpath.unlink(missing_ok=True)
        print(f"  TB open: {bid} ({tid})")

    # Beads may auto-close the parent epic when all children were closed before TB was added.
    if not dry and open_tb_items:
        run_bd(
            [
                "reopen",
                prog_id,
                "-r",
                "Program umbrella stays open while technical backlog items remain (see TB under this epic).",
            ],
            dry_run=False,
        )

    print("\nDone.")
    if not dry:
        print(run_bd(["status"], dry_run=False))
        print(run_bd(["list", "--all", "--limit", "50"], dry_run=False))


if __name__ == "__main__":
    try:
        main()
    except Exception as ex:
        print(f"ERROR: {ex}", file=sys.stderr)
        sys.exit(1)
