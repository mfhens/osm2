# ADR-0005: Keycloak as Central OIDC/OAuth2 Identity Provider

**Status**: Accepted
**Date**: 2025-07-01
**Deciders**: Architecture Team

## Context

osm2 serves three distinct user populations with different authentication requirements:

1. **Taxable persons and intermediaries** — authenticate via MitID/NemLog-in (Danish national digital identity).
2. **Skatteforvaltningen caseworkers** — authenticate via OCES3 (Danish public-sector employee certificates).
3. **Services** — machine-to-machine calls between backend services require a non-interactive credential flow.

Implementing MitID/NemLog-in and OCES3 integration directly in each service would duplicate authentication logic across 6 services and expose each to changes in national identity infrastructure. A central identity provider is required to isolate this complexity.

## Decision

Keycloak 24.0 is the central OIDC/OAuth2 identity provider for osm2.

- Taxable persons and intermediaries authenticate via **Authorization Code Flow with PKCE**, with MitID/NemLog-in proxied through Keycloak as an Identity Provider (IdP). Keycloak handles the MitID protocol details; services see only a standard JWT.
- Caseworkers authenticate via **OCES3** through a Keycloak IdP mapper. Client certificate extraction is handled at the Keycloak level.
- Service-to-service calls use the **Client Credentials Flow**. The resulting token carries the `SERVICE` role, which gates access to `/internal/**` endpoints on all backend services.

All backend services validate JWT bearer tokens via Spring Security's `spring-boot-starter-oauth2-resource-server`. The JWKS endpoint is `http://keycloak:8080/realms/osm2/protocol/openid-connect/certs`.

The realm configuration is version-controlled in `config/keycloak/osm2-realm.json` and imported automatically on Keycloak container startup via `--import-realm`.

Keycloak roles mapped to osm2 business roles:

| Keycloak Role | Audience |
|---|---|
| `TAXABLE_PERSON` | Taxable persons filing their own returns |
| `INTERMEDIARY` | Agents filing on behalf of taxable persons |
| `CASEWORKER` | Skatteforvaltningen internal staff |
| `SERVICE` | Backend service accounts |

## Consequences

**Positive**
- Single realm for all user types: one token issuer, one JWKS endpoint for all services to validate against.
- Keycloak handles MFA, session management, token refresh, and logout — services have no direct MitID or OCES3 dependency.
- MitID integration changes are isolated to Keycloak configuration; services require no code changes.
- The realm JSON export provides a reproducible, auditable identity configuration.

**Negative**
- Keycloak is a critical dependency: if Keycloak is unavailable, no user or service can authenticate. Must be deployed with high availability in production (active-passive minimum).
- Keycloak upgrade cycles must be coordinated with the MitID/NemLog-in IdP configuration, which may have compatibility constraints.
- The realm export must be kept in sync with live Keycloak configuration; divergence is a risk in environments with manual configuration changes.
