# ADR-0025: Maven as Build Tool

**Status**: Accepted
**Date**: 2025-07-01
**Deciders**: Architecture Team

## Context

osm2 is a multi-module Java 21 project with a shared library (`osm2-common`), 6 backend services, and 2 portal BFFs. The build tool must support: reproducible builds across developer workstations and CI; centralised dependency version management to prevent version conflicts across 8 modules; enforced code quality gates (test coverage, security scanning, style); and straightforward CI integration with GitHub Actions or equivalent pipelines. The team has existing Maven expertise.

## Decision

**Maven ≥ 3.9.0** with a multi-module layout is the build tool for osm2. No Gradle.

The parent POM (`pom.xml` at repository root, `artifactId: osm2-parent`) is the authoritative source for:

- **Dependency versions** — all version numbers are declared as `<properties>` (e.g., `<spring-boot.version>`, `<resilience4j.version>`) and referenced via the Spring Boot BOM import. No version declared in child POM `<dependencies>`.
- **Spring Boot BOM** — imported via `<dependencyManagement>` to inherit Spring Boot's curated dependency set.
- **Plugin configuration** — all plugins configured in `<pluginManagement>`:
  - `spotless-maven-plugin` — code formatting validation in the `validate` phase (Google Java Format).
  - `jacoco-maven-plugin` — code coverage in the `verify` phase; minimum 80% line coverage gate on `osm2-common`.
  - `maven-enforcer-plugin` — gates: Java 21+, Maven 3.9+, `dependencyConvergence` (no duplicate transitive version conflicts).
  - `maven-surefire-plugin` — unit test execution.
  - `maven-failsafe-plugin` — integration test execution (`*IT.java`).
  - `owasp-dependency-check-maven` — vulnerability scanning, activated by `-P security-scan` profile (run in CI, not on every build).
  - `spring-boot-maven-plugin` — executable JAR and OCI image packaging (`build-image` goal).

The **Maven Wrapper** (`mvnw` / `mvnw.cmd`) is committed to the repository, ensuring that builds use a pinned Maven version regardless of the developer's locally installed Maven.

## Consequences

**Positive**
- Reproducible builds: the Maven wrapper pins the Maven version; BOM import pins transitive dependency versions.
- The enforcer plugin prevents accidental Java/Maven version downgrade and dependency version conflicts in CI.
- JaCoCo gates enforce minimum test coverage; Spotless gates enforce consistent formatting without style review comments in PRs.
- OWASP Dependency Check in the `security-scan` profile provides CVE scanning without slowing down the standard build cycle.

**Negative**
- Maven XML is verbose: adding a new dependency or plugin requires more boilerplate than Gradle's Kotlin DSL.
- Incremental build performance is slower than Gradle for large multi-module projects. At current scale (8 modules) this is acceptable; at 20+ modules a migration to Gradle or build caching should be reconsidered.
- The `dependencyConvergence` enforcer rule occasionally produces false positives for legitimate multi-version classpaths (e.g., `slf4j-api`). Exclusions must be explicitly documented in the parent POM.
