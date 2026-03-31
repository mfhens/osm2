# ADR-0011: PostgreSQL as Primary Relational Database

**Status**: Accepted
**Date**: 2025-07-01
**Deciders**: Architecture Team

## Context

osm2 processes structured VAT return data, transactional payment records, and scheme registration state — all of which require strong relational guarantees (referential integrity, ACID transactions, complex reporting queries). The data model is well-understood and primarily relational. NoSQL alternatives would add complexity without benefit for this workload. Schema-level isolation between services is a hard requirement (ADR-0007).

## Decision

PostgreSQL 18 (Alpine variant) is the primary relational database for all osm2 services.

**Schema isolation**: Each service uses a dedicated PostgreSQL database instance (in development, separate schemas within a shared instance). No service shares a database with another (ADR-0007).

**Schema migrations**: Flyway 11.20.3 manages schema migrations. All migration scripts follow the naming convention `V{n}__{description}.sql` and are stored in `src/main/resources/db/migration/` within each service module. Flyway runs automatically on application startup. Baseline migrations are checked into source control and reviewed as part of the PR process.

**ORM**: Spring Data JPA with Hibernate 6.x is the ORM layer. Native SQL queries are used for complex reporting queries where JPQL is insufficient.

**Connection pooling**: HikariCP (bundled with Spring Boot). Default configuration: `maximum-pool-size=10`, `minimum-idle=2`, `connection-timeout=30000ms`. Production values are environment-specific and set via Kubernetes ConfigMap.

**Extensions used**:
- `uuid-ossp` for UUID primary key generation (server-side).
- `pg_trgm` in records-service for trigram-based text search on return reference numbers.

## Consequences

**Positive**
- Strong ACID guarantees for payment and registration transactions.
- Schema-per-service isolation directly supports ADR-0007 and ADR-0014 (GDPR PII silo).
- Flyway provides repeatable, auditable, version-controlled schema migrations. Rollback is explicit (downgrade scripts where required).
- PostgreSQL's rich query planner handles the complex aggregation queries required for OSS return reconciliation reports.
- Alpine image minimises container attack surface and image size.

**Negative**
- PostgreSQL major version upgrades (e.g., 18 → 19) require coordinated data directory migration and application release. Must be planned.
- Flyway baseline must be managed carefully if production databases are initialised out-of-band.
- HikariCP pool sizing must be tuned per environment; default values are conservative and may be insufficient under peak OSS filing load.
