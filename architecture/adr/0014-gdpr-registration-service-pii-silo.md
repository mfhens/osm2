# ADR-0014: GDPR — Registration-Service as Sole PII Silo

**Status**: Accepted
**Date**: 2025-07-01
**Deciders**: Architecture Team

## Context

The EU General Data Protection Regulation (Regulation 2016/679) and the Danish Data Protection Act (Databeskyttelsesloven) require that personal data be minimised, isolated, and access-controlled. osm2 processes taxable person identity data — VAT registration numbers (CVR), legal names, addresses, and contact details — necessary for OSS scheme registration. This data constitutes personal data under GDPR Article 4(1) as it relates to identifiable legal persons and their representatives.

Without architectural enforcement of data minimisation, PII tends to spread across service databases, log files, and API responses as the system evolves, creating compliance risk and complicating data subject rights fulfilment.

## Decision

The `osm2-registration-service` is the **sole PII silo** for osm2. It is the only service that may store, process, or transmit taxable person identity fields (CVR number, legal name, address, contact details, representative identity).

All other services reference taxable persons exclusively via an opaque `registrant_id` UUID. This UUID is assigned by registration-service at the time of registration and carries no decodable personal information.

**Prohibitions** (enforced by ArchUnit and code review):
- No service other than registration-service may persist taxable person identity fields in its database schema.
- No service other than registration-service may log taxable person identity fields (e.g., in structured log output or exception messages).
- No service other than registration-service may include taxable person identity fields in its API responses (responses carry only `registrant_id`).

**CPR numbers are not processed**: osm2 is a business tax system operating on legal entities. CPR (personal civil registration) numbers are not collected or processed at any point.

The GDPR register documenting all personal data processing activities is maintained in `compliance/gdpr-register.csv` and reviewed on every release.

## Consequences

**Positive**
- GDPR data minimisation (Article 5(1)(c)) is enforced at the architecture level, not just policy level.
- A data breach in any service other than registration-service cannot expose taxable person identity data — other services hold only UUIDs.
- Data subject rights (Article 15 access, Article 17 erasure, Article 20 portability) are implemented in a single service, not distributed across 6 services.
- GDPR impact assessment scope is reduced: only registration-service requires full PII handling review.

**Negative**
- Registration-service is a critical dependency. If it is unavailable, no new registrations can be created and no identity lookups can be performed. Must be deployed with high availability.
- Portal BFFs that display registrant names must call registration-service for every display operation — this is a deliberate design constraint, not a performance optimisation opportunity.
- ArchUnit tests must be maintained to catch accidental PII field introduction in other services' entity classes. The prohibited field list must be kept current.
