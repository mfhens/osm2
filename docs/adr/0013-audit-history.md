# ADR-0013: Audit History via audit-trail-commons

**Status**: Accepted
**Date**: 2025-07-01 (updated 2026-03-31)
**Deciders**: Architecture Team

## Context

VAT administration in Denmark requires full auditability of data mutations. EU OSS regulations (Council Directive 2006/112/EC, as amended) mandate traceable records of who created or modified scheme registrations, VAT return submissions, and payment records — and when. Without a consistent audit mechanism, individual services would implement ad-hoc approaches, leading to gaps and inconsistencies across the audit trail.

## Decision

All osm2 JPA entities that require auditing extend `AuditableEntity` from the `audit-trail-commons` library (`dk.ufst:audit-trail-commons:1.0`). This replaces the earlier hand-rolled `dk.osm2.common.entity.AuditableEntity`, which was incomplete (missing `updatedBy` and `version`).

The library is added as a dependency of `osm2-common` and flows transitively to all five backend services.

`AuditableEntity` is an abstract `@MappedSuperclass` providing the following columns on every inheriting entity:

| Column | Java type | Population |
|---|---|---|
| `created_at` | `LocalDateTime` | Set on insert via Hibernate `@CurrentTimestamp(event=INSERT)` |
| `updated_at` | `LocalDateTime` | Updated on every change via Hibernate `@CurrentTimestamp` |
| `created_by` | `String` | Set on insert via Spring Data `@CreatedBy` |
| `updated_by` | `String` | Updated on every change via Spring Data `@LastModifiedBy` |
| `version` | `Long` | Optimistic-lock counter via `@Version` |

The `created_by` / `updated_by` fields are populated via an `AuditorAware<String>` bean auto-configured by the library. It extracts the `sub` claim from the JWT in the current `SecurityContext`. For unauthenticated requests (demo / dev / local profiles — permissive filter chain), it returns `"anonymous"`.

**HTTP request audit context** is set by `AuditContextFilter` (also from the library, `@Order(100)`). For each HTTP request it calls:

```sql
SELECT public.set_audit_context(userId, clientIp::inet, applicationName)
```

This PostgreSQL function (created in each service's database via the V2 Flyway migration) stores `userId`, `clientIp`, and `applicationName` as session-level config variables. These are available to any database trigger that needs to enrich audit records. Common Logging System (CLS) event shipping via `ClsAuditClientImpl` is disabled by default (`opendebt.audit.cls.enabled: false`) and must be activated in production environments.

**Full change history** for registration and payment entities is provided by Hibernate Envers (`@Audited` annotation). Envers writes a revision record to `{table}_AUD` on every insert, update, and delete. This enables point-in-time reconstruction of any registration or payment record.

Payment mutations are additionally recorded in immudb (ADR-0029) for tamper-evident financial audit, providing a cryptographic proof chain that Envers alone cannot provide.

**SOAP PII masking** for payment-service's NemKonto and fordringssystem integrations uses `SoapPiiMaskingUtil` (also from the library), which masks CPR numbers, bank account numbers, and names in SOAP XML before logging (see ADR-0034).

## Consequences

**Positive**
- Consistent audit columns across all services — no per-service variation in column names or types.
- `AuditContextFilter` stamps every HTTP request with user / IP / application name at the PostgreSQL session level; database triggers can reference these without any application-layer change.
- Envers provides a queryable, structured change history without requiring manual history tables.
- The combination of Envers (queryable) and immudb (tamper-evident) satisfies both operational and regulatory audit requirements for payment records.
- CLS integration is ready to activate via a single property flip; no code changes required.

**Negative**
- `audit-trail-commons` uses `LocalDateTime` for timestamp fields; DDL columns are `TIMESTAMPTZ`. PostgreSQL JDBC handles the conversion transparently using the connection's session timezone (UTC in all osm2 environments), but it is a semantic mismatch — future library updates should align to `Instant`.
- `AuditorAware` returns `"anonymous"` when the SecurityContext is empty (demo / dev profiles). Audit data for demo seed records will show `created_by = 'system'` (the Flyway migration default) rather than a real user.
- Envers `_AUD` tables grow indefinitely; a retention policy must be defined and implemented (outside the scope of this ADR).
- The `set_audit_context()` function must exist in each service's PostgreSQL database before the application starts. This is guaranteed by the V2 Flyway migration, but if a service is started against a database where only V1 has run, the application will fail on the first HTTP request (graceful degradation: filter logs a warning and continues, per library design).
