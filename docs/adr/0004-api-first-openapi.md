# ADR-0004: API-First Design with OpenAPI 3.1

**Status**: Accepted
**Date**: 2025-07-01
**Deciders**: Architecture Team

## Context

osm2 integrates with multiple external systems: member-state tax authority APIs, Keycloak (OIDC), immudb (gRPC/REST), and the ECB exchange rate feed. Internally, 6 backend services call each other via REST. Without a contract-first approach, API drift between producer and consumer is inevitable — particularly during parallel development by separate teams. Integration bugs discovered late in the development cycle are costly in a regulated system where API changes may require conformance testing against EU member-state requirements.

## Decision

All osm2 REST APIs are defined API-first using the OpenAPI 3.1 specification. SpringDoc OpenAPI 2.5.0 generates the OpenAPI document at runtime from annotations (`@Operation`, `@ApiResponse`, `@Schema`). Every service exposes:

- `GET /api-docs` — machine-readable OpenAPI JSON
- `GET /swagger-ui.html` — interactive API documentation

Both endpoints are disabled in the `production` Spring profile via `springdoc.api-docs.enabled=false` and `springdoc.swagger-ui.enabled=false`.

Internal service-to-service APIs use the path prefix `/internal/**` and are secured with `hasRole('SERVICE')` — only Keycloak service accounts may call them. External/portal-facing APIs use `/api/**` and are secured with `hasRole('TAXABLE_PERSON')`, `hasRole('INTERMEDIARY')`, or `hasRole('CASEWORKER')` as appropriate.

OpenAPI schemas for shared types (e.g., `RegistrantId`, `SchemeType`, `ReturnPeriod`) are defined in osm2-common and referenced from individual service specs.

## Consequences

**Positive**
- Consumer-driven contract testing is possible: consumers can validate their expectations against the published OpenAPI document.
- API documentation is always in sync with the implementation — no separate documentation artefact to maintain.
- The OpenAPI document can be used to generate client SDKs for member-state integration tests.
- Internal vs. external API boundaries are explicit and enforced by Spring Security path matchers.

**Negative**
- Annotation discipline is required: developers must keep `@Operation` and `@Schema` annotations accurate and complete.
- SpringDoc must be kept aligned with Spring Boot upgrades; breaking changes between versions have occurred historically.
- API-first discipline adds upfront design time before implementation can begin.
