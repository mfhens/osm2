# Agents — osm2

This file documents the AI agents available in this project. It is maintained by `implementation-doc-sync` and updated whenever agents are added, removed, or meaningfully changed.

## Pipeline Agents

| Agent | Role | Triggered by |
|---|---|---|
| `delivery-orchestrator` | Routes delivery work to the correct pipeline based on available artifacts | User request |
| `pipeline-conductor` | Orchestrates the end-to-end delivery pipeline in sequence | `delivery-orchestrator` |
| `backlog-planner` | Analyzes petitions, infers dependencies, produces phased backlog | `delivery-orchestrator` |
| `sprint-tracker` | Reconciles sprint-level petition delivery status | `pipeline-conductor` |

## Requirements Agents

| Agent | Role |
|---|---|
| `prompt-to-requirements` | Converts plain-language requests into petition + outcome-contract + Gherkin |
| `petition-translator` | Extracts Gherkin scenarios from petition documents |
| `petition-translator-reviewer` | Validates Gherkin translations for faithfulness and completeness |
| `petition-to-gherkin` | Converts petition + outcome contract into a Gherkin feature file |

## Architecture Agents

| Agent | Role |
|---|---|
| `component-assigner` | Maps functional scenarios to responsible components |
| `application-architect` | Reviews scenario-to-component mappings for functional cohesion |
| `solution-architect` | Transforms requirements into solution architecture + Structurizr DSL block |
| `solution-architecture-reviewer` | Gate-keeps architecture for petition alignment, simplicity, and DSL compliance |
| `c4-model-validator` | Validates Structurizr DSL syntax, structural completeness, and policy compliance |
| `c4-architecture-governor` | Enforces architecture at PR gate (PR mode) and deployment gate (drift-detection mode) |

## Specification & Implementation Agents

| Agent | Role |
|---|---|
| `specs-translator` | Converts requirements + architecture into testable implementation specs |
| `specs-reviewer` | Validates specs for traceability and completeness |
| `specs-minimality-reviewer` | Eliminates bloat and speculation from specs |
| `bdd-test-generator` | Generates failing BDD step definitions |
| `bdd-test-coverage-auditor` | Audits test coverage against requirements |
| `unit-test-minimality-reviewer` | Ensures unit tests are minimal and requirement-justified |
| `tdd-enforcer` | Implements code under strict TDD discipline |
| `tech-debt-executor` | Executes technical backlog items |

## Review & Quality Agents

| Agent | Role |
|---|---|
| `code-reviewer-strict` | Validates implementation against petition, specs, and coding principles |
| `code-minimality-reviewer` | Eliminates unnecessary code |
| `gherkin-minimality-reviewer` | Ensures Gherkin scenarios are petition-justified |
| `scrutiny-feature-reviewer` | Deep review of completed feature implementations |
| `implementation-doc-sync` | Enforces documentation updates when code changes occur |

## Compliance Agents

| Agent | Role |
|---|---|
| `gdpr-database-compliance-auditor` | Audits database schemas for GDPR compliance |
| `rigsarkivet-compliance-data-assessor` | Assesses persisted data for Danish National Archives obligations |

## Utility Agents

| Agent | Role |
|---|---|
| `meta-agent` | Generates new agent configurations from natural language descriptions |
| `project-bootstrap` | Bootstraps project infrastructure for the SDLC pipeline |
| `worker` | Generic task executor |
| `translator` | Translates Danish Spring message bundles to target locales |

## C4 Architecture Governance

Architecture is governed using Structurizr DSL. Key files:

| File | Purpose |
|---|---|
| `architecture/workspace.dsl` | Canonical C4 model — updated by `solution-architect`, maintained by `implementation-doc-sync` |
| `architecture/policies.yaml` | Architecture policy set — evaluated by `c4-model-validator` and `c4-architecture-governor` |

See `architecture/adr/` for architectural decision records that govern all design choices.

## Component Definitions

Component definitions live in `design/components/`. Each `.yaml` file describes a bounded context or deployable unit. The `component-assigner` agent reads these to map petition scenarios to responsible components.

> [STUB — create `design/components/<component-name>.yaml` files as the architecture is elaborated and components are formally defined]

## Wasteland Integration

The **wasteland** (`mfhens/ufst` on DoltHub) is the federated work registry for the
UFST Modernization programme. It operates at petition/TB granularity and is visible to
external contractors and agent rigs that join the federation.

**Beads and wasteland coexist.** Beads is the inner project tracker (sprint subtasks,
fine-grained status). The wasteland is the outer federated registry (petition-level bounty
board with evidence and trust stamps).

Local clone: `~/.hop/commons/mfhens/ufst`

### Claim

Before starting work on a petition or TB item, mark it claimed in the wasteland:

```bash
cd ~/.hop/commons/mfhens/ufst
dolt pull origin main
dolt sql -q "UPDATE wanted SET status='in_progress', claimed_by='mfhens' \
  WHERE id='<petition-id>' AND status='open'"
dolt add . && dolt commit -m 'claim: <petition-id>' && dolt push origin main
```

If the item is not yet in `wanted` (new TB item posted mid-sprint), INSERT it first — see **New items** below.

### Complete

When a petition or TB item is marked `implemented`/`done` in `program-status.yaml`, post
the completion and close the wanted item:

```bash
cd ~/.hop/commons/mfhens/ufst
GIT_SHA=$(git -C /home/markus/GitHub/osm2 rev-parse --short HEAD)
dolt sql -q "INSERT IGNORE INTO completions \
  (id, wanted_id, completed_by, evidence, completed_at) \
  VALUES ('<petition-id>-cmp', '<petition-id>', 'mfhens', 'commit:${GIT_SHA}', NOW())"
dolt sql -q "UPDATE wanted SET status='done' WHERE id='<petition-id>'"
dolt add . && dolt commit -m 'complete: <petition-id>' && dolt push origin main
```

### Stamp

Reviewer agents (`code-reviewer-strict`, `scrutiny-feature-reviewer`) issue stamps on
**external** workers' completions to signal verified quality. The `stamps` table enforces
`author != subject`, so self-stamps are not allowed — stamps are only meaningful when
external rigs are involved.

```bash
cd ~/.hop/commons/mfhens/ufst
STAMP_ID=$(python3 -c "import uuid; print(str(uuid.uuid4())[:16])")
dolt sql -q "INSERT INTO stamps \
  (id, author, subject, valence, confidence, skill_tags, message, context_id, context_type, created_at) \
  VALUES ('${STAMP_ID}', '<reviewer-handle>', '<worker-handle>', \
  '{\"quality\":\"high\",\"correctness\":\"verified\"}', 0.9, \
  '[\"java\",\"spring-boot\",\"osm2\"]', '<one-line review summary>', \
  '<completion-id>', 'completion', NOW())"
dolt add . && dolt commit -m 'stamp: <worker-handle> on <completion-id>' && dolt push origin main
```

### New items

**New TB item** (`backlog-planner` posting to wasteland after updating `program-status.yaml`):

```bash
cd ~/.hop/commons/mfhens/ufst
dolt sql -q "INSERT IGNORE INTO wanted \
  (id, title, project, type, priority, tags, posted_by, status, effort_level, sandbox_required, created_at, updated_at) \
  VALUES ('<TB-id>', '<title>', 'osm2', 'technical_backlog', 3, \
  '[\"java\",\"tech-debt\",\"osm2\"]', 'mfhens', 'open', 'medium', TRUE, NOW(), NOW())"
dolt add . && dolt commit -m 'post: <TB-id>' && dolt push origin main
```

**New ADR** (`solution-architect` after writing an accepted ADR):

```bash
cd ~/.hop/commons/mfhens/ufst
dolt sql -q "INSERT IGNORE INTO decisions \
  (id, number, title, status, summary, skill_tags, project, created_at) \
  VALUES ('adr-<NNNN>', <N>, '<ADR title>', 'Accepted', '<one-sentence decision>', \
  '[\"architecture\",\"adr\"]', 'osm2', NOW())"
dolt add . && dolt commit -m 'decision: adr-<NNNN>' && dolt push origin main
```

<!-- BEGIN BEADS INTEGRATION v:1 profile:minimal hash:ca08a54f -->
## Beads Issue Tracker

This project uses **bd (beads)** for issue tracking. Run `bd prime` to see full workflow context and commands.

### Quick Reference

```bash
bd ready              # Find available work
bd show <id>          # View issue details
bd update <id> --claim  # Claim work
bd close <id>         # Complete work
```

### Rules

- Use `bd` for ALL task tracking — do NOT use TodoWrite, TaskCreate, or markdown TODO lists
- Run `bd prime` for detailed command reference and session close protocol
- Use `bd remember` for persistent knowledge — do NOT use MEMORY.md files

## Session Completion

**When ending a work session**, you MUST complete ALL steps below. Work is NOT complete until `git push` succeeds.

**MANDATORY WORKFLOW:**

1. **File issues for remaining work** - Create issues for anything that needs follow-up
2. **Run quality gates** (if code changed) - Tests, linters, builds
3. **Update issue status** - Close finished work, update in-progress items
4. **PUSH TO REMOTE** - This is MANDATORY:
   ```bash
   git pull --rebase
   bd dolt push
   cd ~/.hop/commons/mfhens/ufst && dolt push origin main  # sync wasteland
   git push
   git status  # MUST show "up to date with origin"
   ```
5. **Clean up** - Clear stashes, prune remote branches
6. **Verify** - All changes committed AND pushed
7. **Hand off** - Provide context for next session

**CRITICAL RULES:**
- Work is NOT complete until `git push` succeeds
- NEVER stop before pushing - that leaves work stranded locally
- NEVER say "ready to push when you are" - YOU must push
- If push fails, resolve and retry until it succeeds
<!-- END BEADS INTEGRATION -->
