# Basecamp integration (osm2)

Stakeholder-facing mirror of **`petitions/program-status.yaml`**, Beads, and ADRs in Basecamp — without duplicating canonical truth in Basecamp.

## Principles

1. **Repo + Beads stay authoritative.** Basecamp is for readability, assignment, and discussion.
2. **Stable IDs.** Prefer title prefixes (`OSS-01`, `TB-001`) for idempotent scripts; store Basecamp recording IDs in optional mapping files only when needed.
3. **Automation publishes; humans triage.** Scripts create or move work items; people adjust cards, assignees, and comments.

## One-time setup

1. Install the [Basecamp CLI](https://github.com/basecamp/basecamp-cli) and authenticate: `basecamp auth login`.
2. Copy **`.basecamp/config.example.json`** to **`.basecamp/config.json`** and fill in:
   - **`project_id`** — project ID or name (`basecamp projects list`).
   - **`card_table_id`** — Kanban board for petitions (`basecamp cards columns --in <project>`).
   - **`todolist_id`** — list for technical backlog todos (`basecamp todolists list --in <project>`).
   - **`columns`** — map each `program-status.yaml` petition **status** to a **column ID** (same command as above).
3. Trust the repo config: `basecamp config trust` (see CLI docs — prevents cloned config from hijacking OAuth context).

Optional: set **`BASECAMP_PROJECT`** instead of `project_id` in CI or local shells.

## Scripts (`scripts/basecamp/`)

| Script | Purpose |
|--------|---------|
| **`Publish-SteerCoMessage.ps1`** | Builds a SteerCo-aligned Markdown digest from `program-status.yaml` (phases, critical path, `next_step`, TB, escalations, completion %) and posts it as a **Message**. |
| **`Sync-PetitionBoard.ps1`** | Creates petition **cards** if missing (`OSS-XX — title`) and **moves** them to the column for their current status. |
| **`Sync-TechnicalBacklog.ps1`** | Ensures each `TB-*` row exists as a **todo** on the configured todolist; completes the todo when YAML status is closed. |
| **`Sync-BeadsCommentsToBasecamp.ps1`** | Pushes Beads comments (filtered by keyword, or all) to a Basecamp **card** via a recording ID map. |

Helper: **`read_program_status.py`** — prints YAML as JSON for tooling (used by the PowerShell scripts).

### Examples

```powershell
# Preview digest (no API calls except none)
.\scripts\basecamp\Publish-SteerCoMessage.ps1 -DryRun

# Post SteerCo message (uses .basecamp/config.json)
.\scripts\basecamp\Publish-SteerCoMessage.ps1 -NoSubscribe

# Plan board sync (reads cards; no create/move)
.\scripts\basecamp\Sync-PetitionBoard.ps1 -DryRun

.\scripts\basecamp\Sync-PetitionBoard.ps1

.\scripts\basecamp\Sync-TechnicalBacklog.ps1

# Beads → Basecamp (needs card recording ID)
.\scripts\basecamp\Sync-BeadsCommentsToBasecamp.ps1 -IssueId "osm2-123.1" -CardRecordingId "9876543210" -DryRun
```

Copy **`scripts/basecamp/card-map.example.json`** to **`scripts/basecamp/card-map.json`** and map petition IDs to card recording IDs so you can use **`-PetitionId OSS-01`** instead of a raw ID.

## Status → Kanban columns

Map YAML petition statuses to your board columns in **`columns`**:

| `program-status.yaml` | Typical column |
|----------------------|----------------|
| `draft` | Backlog / Discovery |
| `requirements` | Ready / Spec |
| `in_progress` | In progress |
| `implemented` | Done |
| `blocked` | Blocked |

Use **`default`** for any status not listed (optional).

## Gauges (project progress)

The digest includes **completion %** (petitions with status `implemented` or `validated`). The Basecamp CLI can **create** needles (`basecamp gauges create --position <0–100>`) but **does not move** needle position on update — only the description. For automation, either manage the gauge in the UI or adopt a **delete + create** needle pattern (see `basecamp gauges delete` / `create` in the Basecamp CLI help).

## Privacy

Do not sync secrets, personal data, or unpublished legal text without review; prefer repo paths and short summaries.

## Older doc path

The former single-file write-up was **`docs/integrations/basecamp-showcase.md`** (now a short pointer here).
