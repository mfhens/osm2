# Architecture Overview — osm2

**Status**: [STUB — update after running `solution-architect` for the first time]

For the machine-readable architecture model, see `architecture/workspace.dsl`.
For architecture policies, see `architecture/policies.yaml`.
For architectural principles and decisions, see `docs/adr/`.

---

## System Context

osm2 is a second-generation geospatial data processing system built around OpenStreetMap (OSM) data. It ingests raw OSM dumps and changeset feeds, transforms and enriches them into normalised geodata, and exposes the results to downstream consumers via an API layer.

> [STUB — replace or expand with a precise description of osm2's real purpose, stakeholders, and operational boundaries once confirmed by the team]

**Primary actors:**
- **User** — GIS analysts, data engineers, or application consumers querying processed geodata
- **Operator** — responsible for scheduling ingestion runs and monitoring pipeline health

**External dependencies:**
- **OpenStreetMap** — public source of PBF/XML geodata dumps and changeset feeds

> [STUB — list additional external systems as they are confirmed]

---

## Container Overview

| Container | Technology | Responsibility |
|---|---|---|
| Ingestion Service | Python / Apache Airflow | Fetches and validates raw OSM files; queues for processing |
| Processing Engine | Python / GeoPandas | Transforms raw geometries and tags into normalised geodata |
| Data Store | PostgreSQL / PostGIS | Primary persisted store for enriched geodata |
| API Service | Python / FastAPI | REST / vector-tile interface for downstream consumers |

> [STUB — update technology choices and add containers as the architecture is elaborated. The above reflects the initial stub in `architecture/workspace.dsl`.]

---

## Key Architectural Decisions

See `docs/adr/` for the full decision log.

| ADR | Title | Status |
|---|---|---|
| [ADR-0001](adr/0001-architecture-principles.md) | Architecture Principles | Accepted |

> [STUB — add new rows as ADRs are created]

---

## Data & Privacy Notes

osm2 may process OSM changeset metadata that includes contributor identifiers (usernames, user IDs). The GDPR classification of this data is **unknown at project inception** and must be resolved before the first production deployment. See `compliance/gdpr-register.csv`.

> [STUB — update once the data privacy assessment is complete]
