# ADR-0013: Audit History via AuditableEntity and Envers

**Status**: Accepted
**Date**: 2025-07-01
**Deciders**: Architecture Team

## Context

VAT administration in Denmark requires full auditability of data mutations. EU OSS regulations (Council Directive 2006/112/EC, as amended) mandate traceable records of who created or modified scheme registrations, VAT return submissions, and payment records — and when. Without a consistent audit mechanism, individual services would implement ad-hoc approaches, leading to gaps and inconsistencies across the audit trail.

## Decision

All osm2 JPA entities that require auditing extend `AuditableEntity` from the `osm2-common` module.

`AuditableEntity` is an abstract `@MappedSuperclass` providing the following columns on every inheriting entity:

| Column | Type | Population |
|---|---|---|
| `created_at` | `INSTANT` / `TIMESTAMPTZ` | Set on `@PrePersist` via Spring Data `@CreatedDate` |
| `modified_at` | `INSTANT` / `TIMESTAMPTZ` | Updated on `@PreUpdate` via Spring Data `@LastModifiedDate` |
| `created_by` | `VARCHAR(255)` | Set on `@PrePersist` via Spring Data `@CreatedBy` |

The `created_by` field is populated via a `AuditorAware<String>` implementation registered in each service's Spring context. The implementation extracts the `sub` claim from the JWT in the current `SecurityContext`. For service-to-service calls (using the `SERVICE` role), `created_by` is populated with the client ID of the calling service account.

**Full change history** for registration and payment entities is provided by Hibernate Envers (`@Audited` annotation). Envers writes a revision record to `{table}_AUD` on every insert, update, and delete. This enables point-in-time reconstruction of any registration or payment record.

Payment mutations are additionally recorded in immudb (ADR-0029) for tamper-evident financial audit, providing a cryptographic proof chain that Envers alone cannot provide.

## Consequences

**Positive**
- Consistent audit columns across all services — no per-service variation in column names or types.
- Spring Data handles `created_at` and `modified_at` population automatically; developers cannot forget to set them.
- Envers provides a queryable, structured change history without requiring manual history tables.
- The combination of Envers (queryable) and immudb (tamper-evident) satisfies both operational and regulatory audit requirements for payment records.

**Negative**
- `AuditorAware` must be implemented and registered in each service's Spring context. A missing implementation results in `created_by` being `null`, which is a subtle failure mode.
- Envers doubles the write volume for audited entities (one write to the main table, one to the `_AUD` table). For high-volume entities, this must be considered in capacity planning.
- Envers `_AUD` tables grow indefinitely; a retention policy must be defined and implemented (outside the scope of this ADR).
