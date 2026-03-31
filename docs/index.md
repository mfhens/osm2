# osm2 — One Stop Moms 2

Welcome to the technical documentation for **osm2**, the Danish VAT One Stop Shop administration system.

osm2 implements the EU OSS (One Stop Shop) regime for taxable persons and intermediaries filing VAT returns across EU member states. It supports all three schemes:

- **Non-EU scheme** — for non-EU established businesses
- **EU scheme** — for EU-established businesses
- **Import scheme (IOSS)** — for distance sales of imported goods

## Quick Links

- [Architecture Overview](../architecture/overview.md)
- [Architecture Decision Records](../architecture/adr/0001-architecture-principles.md)
- [OSS-01 — Scheme Eligibility](../petitions/OSS-01/OSS-01.md)

## System Context

osm2 is built on Java 21 / Spring Boot 3.5 / PostgreSQL 18, deployed on Kubernetes, with Keycloak for OIDC authentication and MitID/NemLog-in integration for taxable person identity.

See `architecture/workspace.dsl` for the canonical C4 architecture model.
