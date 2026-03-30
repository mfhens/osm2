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

See `docs/adr/` for architectural decision records that govern all design choices.

## Component Definitions

Component definitions live in `design/components/`. Each `.yaml` file describes a bounded context or deployable unit. The `component-assigner` agent reads these to map petition scenarios to responsible components.

> [STUB — create `design/components/<component-name>.yaml` files as the architecture is elaborated and components are formally defined]
