# ADR-0007: No Cross-Service Database Access

**Status**: Accepted
**Date**: 2025-07-01
**Deciders**: Architecture Team

## Context

Direct database access across service boundaries is the most common cause of accidental coupling in microservices systems. When service A queries service B's database directly, schema changes in B silently break A. It also makes it impossible to enforce GDPR data boundaries at the infrastructure level: if payment-service can query the registration-service schema, the PII silo (ADR-0014) is meaningless. osm2 must enforce bounded-context isolation to support independent schema evolution and regulatory compliance.

## Decision

No osm2 service may connect to a database schema owned by another service.

Each service owns exactly one PostgreSQL database and schema. The owning service is the sole writer and reader of that schema. Cross-service data access is achieved exclusively via authenticated REST API calls over the cluster-internal network (using the service's published API, not a shared database connection).

This rule is enforced at two levels:

1. **ArchUnit tests** in `osm2-common` verify that no service's `DataSource` or `EntityManagerFactory` bean references a JDBC URL belonging to another service's schema. These tests run in the `verify` Maven lifecycle phase.
2. **Architecture policy** `ARCH-001` in `architecture/policies.yaml` explicitly prohibits cross-schema JDBC connections and is checked in the C4 architecture governance pipeline.

Database credentials per service are provisioned separately: each service has its own PostgreSQL user with `CONNECT` privilege only on its own database. This is enforced at the PostgreSQL level in addition to the application-level constraint.

## Consequences

**Positive**
- Schema changes in one service (e.g., adding a column, renaming a table) cannot break another service at the database level.
- GDPR data isolation is enforced at the infrastructure layer: other services cannot query PII from the registration-service schema even if application-level controls fail.
- Each service can evolve its persistence model independently (e.g., switch to a document store) without cross-service impact.

**Negative**
- Inter-service data access requires careful API design. Data that would be a simple JOIN in a monolith requires an API call and handling of network failures.
- Increased latency compared to a direct database join (accepted per non-functional requirements; mitigated by Resilience4j circuit breakers — ADR-0026).
- Aggregation queries that span service boundaries (e.g., return filings with registrant names) must be assembled in the portal BFF layer, not the database.
