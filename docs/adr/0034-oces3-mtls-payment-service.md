# ADR-0034: OCES3 mTLS for Payment Service External System Integration

**Status**: Accepted
**Date**: 2026-03-30
**Deciders**: Architecture Team

## Context

The `osm2-payment-service` must integrate with two external systems operated by Skatteforvaltningen:

- **NemKonto** — the Danish government's clearing system for electronic payment disbursements (refunds to registrants).
- **Fordringssystem** — the debt and claims management system; receives notifications when a registrant owes outstanding VAT after the payment deadline.

Both integrations use SOAP over HTTPS with **mutual TLS (mTLS)** authentication. The calling party must present a valid **OCES3** certificate (Organisationscertifikat) issued by MitID Erhverv (the Danish CA for public-sector and business certificates). The receiving system validates the certificate and extracts the caller's identity from the Subject Distinguished Name (DN).

Implementing raw `javax.net.ssl` / `SSLContext` configuration in the service would require significant boilerplate and create a direct dependency on Java's low-level TLS APIs. OCES3 DN parsing is error-prone and is a shared concern across any service that communicates via the Danish government SOAP infrastructure.

## Decision

The `osm2-payment-service` uses the `oces3-certificate-parser` library (`dk.ufst:oces3-certificate-parser:1.0`) to handle OCES3 X.509 certificate parsing and DN field extraction.

The library is included via the Maven reactor (built before consuming services). It is auto-configured via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` — no explicit `@ComponentScan` is required.

**Configuration** in `osm2-payment-service/src/main/resources/application.yml`:

```yaml
oces3:
  dn-field: ${OCES3_DN_FIELD:CN}
```

The `OCES3_DN_FIELD` environment variable controls which DN component is used to identify the calling system (defaults to `CN`). This enables environment-specific tuning without code changes — e.g. staging environments may use a test CA where the relevant identity is in a different DN field.

**External system relationships** (documented in `architecture/workspace.dsl`):

| From | To | Protocol |
|---|---|---|
| `paymentService` | `nemKonto` | SOAP / OCES3 mTLS |
| `paymentService` | `fordringssystem` | SOAP / OCES3 mTLS |

The `fordringshaverId` (the creditor identifier used in fordringssystem calls) is extracted from the OCES3 certificate's DN field at runtime.

## Consequences

**Positive**
- OCES3 certificate parsing is encapsulated in a shared library; no bespoke TLS/DN parsing code in the service.
- The library's auto-configuration follows the Spring Boot starter pattern — zero boilerplate required in consuming services.
- `dn-field` is environment-overridable, supporting test and staging CA differences without rebuilds.
- NemKonto and fordringssystem are modelled as explicit external systems in the C4 architecture, making the SOAP/mTLS boundary visible and reviewable.

**Negative**
- The `oces3-certificate-parser` library version is pinned; updates to the Danish CA or OCES3 certificate profile require a library upgrade and redeployment.
- mTLS requires the OCES3 private key and certificate chain to be available to the payment service at runtime — these must be injected via Kubernetes Secrets and mounted as a JKS/PKCS12 truststore. The operational procedure for certificate rotation is outside the scope of this ADR.
- Only the payment service currently needs this integration. If other services (e.g. records-service) need SOAP/mTLS in future, they must add the dependency and `oces3.dn-field` configuration independently.
