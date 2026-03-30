# OpenDebt Stack Blueprint

This document describes the full technology stack of OpenDebt. It is intended as a
reusable reference so that a new project of the same type can be bootstrapped by
referencing this blueprint.

**To use**: tell the agent _"Bootstrap a new project using the OpenDebt blueprint"_ and
point it at this file. The `project-bootstrap` droid reads it to understand which modules,
dependencies, and infrastructure components to scaffold.

---

## Language & Runtime

| Concern | Choice | Version |
|---|---|---|
| Language | Java | 21 (LTS, virtual threads ready) |
| Framework | Spring Boot | 3.5.x |
| Build | Maven (multi-module) | ≥ 3.9.0 |
| Enforced | maven-enforcer-plugin | Java 21 + Maven 3.9 hard gates |

---

## Module Structure

One Maven parent (`pom.xml`) with child modules. The pattern splits into three layers:

```
<project>-common/           ← shared DTOs, base entities, test fixtures
<project>-person-registry/  ← GDPR PII silo (encrypted CPR/CVR)
<project>-*-service/        ← domain services (one per bounded context)
<project>-*-portal/         ← BFF portals (one per user type)
<project>-rules-engine/     ← Drools rule execution (if rule-heavy domain)
<project>-integration-gateway/ ← external protocol ingress (EDIFACT, SOAP, etc.)
```

**Invariant: one database schema per service.** Cross-service data access is via REST only — never direct DB joins.

### OpenDebt concrete modules

| Module | Port | Role |
|---|---|---|
| opendebt-common | — | Shared library |
| opendebt-person-registry | 8090 | GDPR PII silo |
| opendebt-debt-service | 8082 | Core debt lifecycle |
| opendebt-creditor-service | 8092 | Creditor master data |
| opendebt-case-service | 8081 | Case + workflow |
| opendebt-payment-service | 8083 | Payment processing |
| opendebt-letter-service | 8084 | Letter/document generation |
| opendebt-wage-garnishment-service | — | Lønindeholdelse |
| opendebt-rules-engine | — | Drools execution |
| opendebt-integration-gateway | — | EDIFACT/SOAP ingress |
| opendebt-creditor-portal | 8085 | BFF for fordringshaver |
| opendebt-caseworker-portal | 8087 | BFF for sagsbehandler |
| opendebt-citizen-portal | 8086 | BFF for skyldner |

---

## Data Layer

| Concern | Choice | Notes |
|---|---|---|
| Database | PostgreSQL 18 (Alpine) | One schema per service |
| Migrations | Flyway 11.20.3 | `V{n}__description.sql`, per-service `classpath:db/migration` |
| ORM | Spring Data JPA + Hibernate | Via Spring Boot BOM |
| Audit history | Custom `AuditableEntity` base class | `created_at`, `modified_at`, `created_by` on all tables |
| Tamper-evident ledger | immudb | gRPC 3322, REST 8094, Prometheus 9497 |

### GDPR isolation pattern

All PII (CPR, CVR, name, address, contact) lives exclusively in the person-registry service,
encrypted at rest with AES-256. Every other service stores only a `person_id` UUID (type
`UUID`, column `person_id`, NOT NULL, no FK across service boundaries).

```java
// CORRECT
@Column(name = "debtor_person_id", nullable = false)
private UUID debtorPersonId;

// WRONG — never in a non-registry service
private String cprNumber;
```

---

## Authentication & Authorisation

| Concern | Choice |
|---|---|
| Identity provider | Keycloak 24.0 (OIDC/OAuth2) |
| Service-to-service | JWT bearer, `hasRole('SERVICE')` on `/internal/**` endpoints |
| Portal users | OIDC Authorization Code Flow via Keycloak |
| Citizens | MitID/TastSelv (proxied through Keycloak in production) |
| M2M legacy ingress | OCES3 mutual TLS at integration-gateway |

Spring Security OAuth2 Resource Server on every service. Keycloak realm exported to
`config/keycloak/` and auto-imported on container start via environment variable
`KEYCLOAK_IMPORT`.

Internal endpoints follow the pattern:
- Path prefix: `/internal/`
- Security: `hasRole('SERVICE')`
- OpenAPI: `@Hidden` (excluded from public Swagger UI)

---

## Specialised Domain Components

| Concern | Library | Version | When to include |
|---|---|---|---|
| Rules engine | Drools | 9.44.0.Final | Declarative eligibility / prioritisation rules |
| Workflow engine | Flowable | 7.0.1 | BPMN process definitions for case workflows |
| EDIFACT parsing | Smooks | 2.0.0 | CREMUL/DEBMUL payment notification messages |
| Double-entry bookkeeping | yanimetaxas/bookkeeping | 4.3.0 | Every financial event → debit+credit journal entry |
| Formal law encoding | Catala | (external compiler) | Legal calculation specifications; oracle layer only |

Catala `.catala_da` files live in `src/main/catala/`. They are compiled to a test oracle
that validates Java output against the formal specification. They are never in the runtime
critical path (ADR-0032).

---

## API Layer

| Concern | Choice |
|---|---|
| Style | REST (Spring MVC) |
| Spec format | OpenAPI 3.1 |
| UI / docs | SpringDoc OpenAPI 2.5.0 (Swagger UI auto-generated) |
| Legacy SOAP | Spring WS 4.0.10 (integration-gateway only) |
| Resilience | Resilience4j 2.4.0 — circuit breaker + retry on all inter-service calls |
| HTTP client | WebClient (reactive) for service-to-service calls |

API-first discipline: OpenAPI spec is the contract; Java code is generated from or validated
against it. No undocumented endpoints.

---

## Portal Pattern (BFF)

Each user-facing portal is a dedicated Spring Boot module with no domain logic:

```
<project>-creditor-portal   → Thymeleaf + Spring MVC + Spring Security (OIDC)
<project>-caseworker-portal → same
<project>-citizen-portal    → same, MitID/TastSelv issuer
```

Portals own session state only. All data fetched from downstream services via `WebClient`.
No direct database access from portals.

---

## Containerisation

### Dockerfile pattern

```dockerfile
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -g 1000 <project> && \
    adduser -u 1000 -G <project> -s /bin/sh -D <project>
WORKDIR /app
COPY --chown=<project>:<project> target/*.jar app.jar
USER <project>
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

Key decisions:
- Non-root user (UID 1000)
- Alpine base (minimal attack surface)
- Container-aware JVM memory (`MaxRAMPercentage=75`)
- No `latest` tag in production; image tagged with Git SHA

### Local development

`docker-compose.yml` starts all services + Keycloak + PostgreSQL.
`docker-compose.observability.yml` starts the full observability stack separately.
Structurizr Lite started via `docker-compose.structurizr.yml`.

Production target: Kubernetes (deployment manifests in `k8s/`).

---

## Observability Stack

```
Service → OTLP/HTTP → OpenTelemetry Collector
                          ├── Tempo      (traces, port 3200)
                          ├── Loki       (logs, port 3100)
                          └── Prometheus (metrics, port 9090)
                                         └── Grafana (dashboards, port 3000)
```

| Concern | Choice |
|---|---|
| Tracing | Micrometer + OTel bridge → Tempo |
| Logging | Logstash Logback Encoder → JSON → Promtail → Loki |
| Metrics | Micrometer → Prometheus |
| Dashboards | Grafana OSS (provisioned from `config/grafana/`) |
| Log shipping | Promtail (Docker socket scrape) |

Every service configures `OTEL_EXPORTER_OTLP_ENDPOINT` as an environment variable.
No code changes needed to switch backends.

---

## Code Quality Gates

| Gate | Tool | Threshold |
|---|---|---|
| Formatting | Spotless (Google Java Format 1.22.0) | Enforced at `validate` phase — build fails on diff |
| Coverage | JaCoCo 0.8.14 | Line ≥ 80%, Branch ≥ 70% |
| Static analysis | SonarCloud | Quality gate enforced in CI |
| Vulnerability scan | OWASP Dependency Check 12.2.0 | CVSS ≥ 7 fails build (`security-scan` Maven profile) |
| Architecture | ArchUnit 1.4.1 | Layer and package rules encoded as tests |
| Dependency conflicts | maven-enforcer `dependencyConvergence` | Convergence failure = build failure |

---

## Testing Strategy

| Level | Tool | Scope |
|---|---|---|
| Unit | JUnit 5 + Mockito | Domain logic, service layer |
| BDD/acceptance | Cucumber 7.18.0 | One `.feature` file per petition; step defs per module |
| Integration | Testcontainers 1.21.4 | Real PostgreSQL in CI via Docker |
| Architecture | ArchUnit | Dependency rules enforced as tests |

BDD feature files live in two places:
- `petitions/<id>.feature` — canonical, traceable to requirements
- `src/test/resources/features/` — in-module, runnable by Maven

Scenarios map 1:1 to acceptance criteria in outcome contracts.

---

## CI/CD (GitHub Actions)

```
push → build (mvn verify)
      → SonarCloud analysis
      → OWASP scan (security-scan profile, NVD API key required)
      → Dependency review (GitHub Action)
      → (on tag) → release workflow → Docker build + push
```

Actions pinned to SHA for supply-chain security:

| Action | Version | Node runtime |
|---|---|---|
| `actions/checkout` | v6 | node24 |
| `actions/setup-java` | v5 | node24 |
| `actions/upload-artifact` | v4 | (node24 pending upstream) |
| `actions/dependency-review-action` | v4 | (node24 pending upstream) |

Required secrets: `SONAR_TOKEN`, `NVD_API_KEY`, `GITHUB_TOKEN` (auto).

---

## Architecture Governance

| Concern | Tool |
|---|---|
| Architecture decisions | ADRs in `docs/adr/` (Markdown, numbered `NNNN-title.md`) |
| C4 model | Structurizr DSL (`architecture/workspace.dsl`) |
| Policy enforcement | `architecture/policies.yaml` |
| Documentation site | MkDocs (`mkdocs.yml`; `docs/site/`) |
| Formal law specs | Catala (`.catala_da` files, Danish locale) |

ADRs are binding constraints. Every architectural decision in code must trace to an ADR or
trigger a new one.

---

## Developer Tooling

| Tool | Version | Purpose |
|---|---|---|
| Lombok | 1.18.32 | Boilerplate elimination (`@Builder`, `@Data`, etc.) |
| MapStruct | 1.6.3 | Type-safe entity ↔ DTO mapping (annotation processor) |
| OkHttp3 | 4.12.0 | HTTP client for integration tests |

---

## Key Architectural Decisions

These ADRs define invariants for any project using this blueprint:

| ADR | Decision |
|---|---|
| 0002 | Microservices architecture |
| 0004 | API-first with OpenAPI 3.1 |
| 0005 | Keycloak for authentication |
| 0006 | Kubernetes as production target |
| 0007 | No cross-service database access |
| 0011 | PostgreSQL as the database |
| 0013 | Enterprise PostgreSQL audit + history |
| 0014 | GDPR data isolation in a dedicated Person Registry |
| 0018 | Double-entry bookkeeping for all financial events |
| 0019 | Orchestration over event-driven (REST calls, not events) |
| 0024 | OTel/Grafana observability stack |
| 0025 | Maven as build tool |
| 0026 | Resilience4j for inter-service resilience |
| 0029 | immudb for tamper-evident financial ledger |
| 0031 | Statutory codes as enums, not configuration tables |
| 0032 | Catala for formal compliance layer (oracle only, not runtime) |

---

## What Makes This Pattern Distinctive

1. **GDPR by architecture** — PII isolation is structural, enforced by the module split.
   It cannot be accidentally violated without crossing a service boundary.

2. **Law as code** — Catala encodes legal rules as testable formal specifications before
   implementation. Used as an oracle, not a runtime dependency.

3. **Petition-driven delivery** — every feature starts as a formal petition → outcome
   contract → Gherkin → code. The backlog is traceable to law (Juridisk Vejledning).

4. **Agent pipeline** — specialist AI agents execute each pipeline phase; a human approves
   at defined boundaries. The pipeline is encoded in `.factory/droids/`.

5. **Statutory codes as enums** — law-mandated values (reason codes, priority rules) are
   compiled into the codebase. They change only when the law changes (ADR-0031).

6. **Double-entry bookkeeping** — every financial event produces a journal entry.
   Financial state is always reconcilable from the ledger alone.

---

## Bootstrap Checklist for a New Project

When using this blueprint to start a new project, verify each item:

- [ ] Maven multi-module parent with enforcer (Java 21, Maven 3.9)
- [ ] `common` module created with shared base classes
- [ ] Person Registry module with encryption configuration
- [ ] One PostgreSQL schema + Flyway migration set per service
- [ ] Keycloak realm exported and auto-import configured
- [ ] `docker-compose.yml` with all services + Keycloak + PostgreSQL
- [ ] `docker-compose.observability.yml` with OTel Collector + Tempo + Loki + Prometheus + Grafana
- [ ] immudb service in docker-compose (if financial ledger required)
- [ ] Spotless configured in root POM with Google Java Format
- [ ] JaCoCo configured (line ≥ 80%, branch ≥ 70%)
- [ ] OWASP Dependency Check in `security-scan` Maven profile
- [ ] SonarCloud project created and `SONAR_TOKEN` secret added
- [ ] GitHub Actions CI workflow with SHA-pinned actions
- [ ] `architecture/workspace.dsl` Structurizr DSL scaffolded
- [ ] `architecture/policies.yaml` created
- [ ] `AGENTS.md` created (agent pipeline documentation)
- [ ] `docs/adr/` directory with ADR-0001 (record decisions)
- [ ] `petitions/program-status.yaml` created (delivery backlog)
- [ ] `.factory/` directory with droid definitions
- [ ] MkDocs site scaffolded (`mkdocs.yml` + `docs/site/`)
- [ ] ArchUnit test in `common` enforcing package rules
- [ ] Non-root user in all Dockerfiles (UID 1000)
