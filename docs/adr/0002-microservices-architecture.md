# ADR-0002: Microservices Architecture

**Status**: Accepted
**Date**: 2025-07-01
**Deciders**: Architecture Team

## Context

osm2 implements 6 distinct regulatory domains: scheme eligibility, registration, returns, payments, records, and portal access. Each domain has materially different scaling, compliance, and lifecycle requirements. Scheme eligibility rules change on legislative cycles; payment processing has strict ACID requirements; portal access is stateless and horizontally scalable. Coupling these domains in a monolith would prevent independent deployment, complicate GDPR isolation (PII confined to registration-service per ADR-0014), and conflate unrelated test and release cycles.

## Decision

osm2 is structured as a microservices architecture. Each of the 6 business domains (OSS-01 through OSS-06) is implemented as an independent Spring Boot service with its own PostgreSQL database schema. Portals (taxable-person-portal, authority-portal) are separate BFF services that aggregate and adapt backend service APIs for their respective frontends. All services communicate via REST APIs. All inter-service and client-facing API calls are authenticated with JWT bearer tokens issued by Keycloak (ADR-0005). The service inventory is:

| Service | Domain |
|---|---|
| osm2-scheme-service | OSS-01 Scheme eligibility |
| osm2-registration-service | OSS-02 Taxable person registration |
| osm2-return-service | OSS-03 VAT return filing |
| osm2-payment-service | OSS-04 Payment processing |
| osm2-records-service | OSS-05 Record-keeping |
| osm2-taxable-person-portal | BFF for taxable persons / intermediaries |
| osm2-authority-portal | BFF for Skatteforvaltningen caseworkers |
| osm2-common | Shared library (no runtime deployment) |

## Consequences

**Positive**
- Independent deployability: each service can be released, rolled back, and scaled without affecting others.
- Isolated failure domains: a crash in return-service does not bring down the registration or payment services.
- GDPR isolation enforced at architecture level: PII is confined to registration-service (ADR-0014); other services hold only opaque `registrant_id` UUIDs.
- Teams can own and evolve individual services independently.

**Negative**
- Distributed tracing is required to correlate requests across service boundaries (addressed by ADR-0024).
- Eventual consistency must be accepted where synchronous calls introduce unacceptable coupling or latency.
- Higher operational overhead compared to a monolith (addressed by Kubernetes — ADR-0006).
- Cross-service API contracts must be maintained explicitly (addressed by API-first — ADR-0004).
