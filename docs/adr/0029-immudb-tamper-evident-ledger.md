# ADR-0029: immudb as Tamper-Evident Payment Ledger

**Status**: Accepted
**Date**: 2025-07-01
**Deciders**: Architecture Team

## Context

Payment records in EU OSS must be tamper-evident. A mutable PostgreSQL record alone — even with Envers change history (ADR-0013) — is insufficient for financial audit requirements because a database administrator with write access could alter both the main table and the Envers audit table. EU OSS payment obligations span multiple jurisdictions and are subject to audit by both Skatteforvaltningen and member-state tax authorities. Cryptographic proof of the integrity of payment records is required.

## Decision

**immudb** is deployed as a Docker container and used by `osm2-payment-service` as a tamper-evident ledger for all payment journal entries.

**Deployment configuration:**
- gRPC port: 3322 (primary client interface)
- REST port: 8094 (health checks and alternative client access)
- Prometheus metrics port: 9497 (scraped by Prometheus — ADR-0024)
- Image: `codenotary/immudb:latest` (pinned to a specific digest in production)

**Write pattern**: For every successful payment mutation written to PostgreSQL (within the PostgreSQL transaction), a corresponding entry is appended to immudb via the `immudb4j` client immediately after the PostgreSQL commit. The immudb write is not part of the PostgreSQL transaction. If the immudb write fails, the payment record is flagged as `PENDING_AUDIT_SYNC` and a reconciliation job retries the immudb write asynchronously.

immudb provides a **cryptographic proof of inclusion** (Merkle tree proof) for each entry. The `immudb4j` client can verify this proof on read, confirming that the entry has not been tampered with since it was written.

**Database configuration:**
- Development: `defaultdb` (immudb built-in default database)
- Production: a dedicated immudb database per environment (`osm2-prod`, `osm2-staging`), provisioned at deployment time

The immudb admin credentials are stored in Kubernetes Secrets (ADR-0006). The payment-service service account uses a restricted immudb user with `write` and `read` permissions only.

## Consequences

**Positive**
- Cryptographic tamper-evidence for payment records: any alteration to an immudb entry is detectable via Merkle proof verification, even by a database administrator.
- Satisfies financial audit requirements without implementing a custom Merkle tree or blockchain infrastructure.
- immudb exposes Prometheus metrics natively, integrating with the existing observability stack (ADR-0024).
- The `PENDING_AUDIT_SYNC` flag and reconciliation job ensure that a transient immudb failure does not block payment processing.

**Negative**
- immudb is an additional operational dependency: it must be deployed, backed up, and monitored separately from PostgreSQL.
- The `immudb4j` client version must be kept compatible with the deployed immudb server version. The immudb team has historically introduced breaking API changes between minor versions.
- The dual-write pattern (PostgreSQL + immudb) is not atomic. The reconciliation job for `PENDING_AUDIT_SYNC` records must be monitored to ensure the two ledgers remain consistent.
- immudb is append-only: correcting an erroneous payment entry requires a compensating entry, not an update or delete. This is correct accounting behaviour but requires developer awareness.
