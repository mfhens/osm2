# ADR-0001: Architecture Principles

**Status**: Accepted
**Date**: 2026-03-30
**Deciders**: [STUB — list names or roles of decision-makers]

## Context

Every software system requires a set of foundational principles to guide architectural decisions consistently. Without explicit principles, each decision is made in isolation and the architecture drifts over time.

This ADR establishes the binding principles for osm2. All agents that produce or review architecture (`solution-architect`, `solution-architecture-reviewer`) treat these as non-negotiable constraints. A deviation from any principle requires a new ADR documenting the rationale.

osm2 is a geospatial data processing system centred on OpenStreetMap data. Its architecture must accommodate large-volume batch ingestion, incremental changeset processing, and low-latency geodata serving — sometimes simultaneously. These constraints make principled separation of concerns especially critical.

## Decision

The following principles govern all architectural decisions for osm2:

1. **Simplicity first**: Choose the minimal architecture that delivers the stated outcome. Complexity requires explicit justification traceable to a requirement.
2. **Traceability**: Every architectural element must trace to a requirement. Orphaned components — elements with no requirement justification — are defects.
3. **Explicit over implicit**: All dependencies, boundaries, and interfaces must be declared in `architecture/workspace.dsl`. Nothing is assumed.
4. **Policy as code**: Architectural constraints are expressed in `architecture/policies.yaml`. Prose documentation alone is insufficient enforcement.
5. **Living model**: `architecture/workspace.dsl` is the source of truth for system topology. Code and IaC that diverge from the declared model are architectural drift and must be resolved, not accepted.
6. **Geodata lineage**: Every transformation step that produces or modifies geodata must be traceable to its upstream source. Lineage must be expressible programmatically, not only in documentation.
7. **Bounded ingestion and serving paths**: Batch ingestion, incremental changeset processing, and API serving are distinct workloads with distinct resource profiles. They must not share a write path to the primary datastore unless an explicit architectural decision permits it.

## Consequences

- Every architecture review (`solution-architecture-reviewer`) checks conformance to these principles explicitly.
- Deviations are not rejected silently — they must be documented as a new ADR and escalated for human review.
- New principles are added via ADR (new file in `architecture/adr/`), not by editing this document.
- [STUB — add osm2-specific principles below this line as the team refines the architecture]
