# ADR-0006: Kubernetes as Deployment Platform

**Status**: Accepted
**Date**: 2025-07-01
**Deciders**: Architecture Team

## Context

osm2 comprises 8 independently deployable runtime artefacts (6 backend services + 2 portal BFFs). Each has different scaling characteristics: the taxable-person-portal handles high seasonal load around OSS return deadlines (quarterly); payment-service requires strict availability guarantees; scheme-service is largely static between legislative changes. Deploying these as processes on virtual machines would require manual scaling decisions, bespoke health check scripts, and risky in-place upgrades. A container orchestration platform is required.

## Decision

osm2 is deployed on Kubernetes. The deployment model is:

- Each service has a `Deployment` manifest in `k8s/<service-name>/` with configurable replica counts.
- **Readiness probes** point to `GET /actuator/health/readiness` (Spring Boot Actuator). Traffic is not routed to a pod until it passes the readiness probe.
- **Liveness probes** point to `GET /actuator/health/liveness`. A failing liveness probe triggers pod restart.
- Services are exposed within the cluster via `ClusterIP` Kubernetes `Service` resources. External access is via an `Ingress` resource backed by the Nginx Ingress controller.
- Secrets (database passwords, Keycloak client secrets, immudb credentials) are stored in Kubernetes `Secret` resources and injected as environment variables. Secrets are never embedded in container images or committed to the repository.
- Container images are built with `./mvnw spring-boot:build-image` (Buildpacks). Image tags are the Maven project version.
- ConfigMaps hold non-sensitive configuration (e.g., ECB rate feed URLs, OTLP endpoint).

Local development uses `docker-compose.yml` (and `docker-compose.observability.yml` for the observability stack). Docker Compose is not a deployment target.

## Consequences

**Positive**
- Independent horizontal scaling per service without infrastructure changes.
- Rolling upgrades with zero downtime: Kubernetes replaces pods one at a time, gating on readiness probes.
- Declarative configuration: the desired state is version-controlled in `k8s/`; drift is detectable.
- Secrets management is centralised in Kubernetes; no secret sprawl across config files.

**Negative**
- Kubernetes operational knowledge is required from the platform team. This is a non-trivial investment.
- Kubernetes adds latency to the local developer feedback loop (mitigated by Docker Compose for local dev).
- Ingress controller configuration (TLS termination, path routing) must be maintained separately per environment.
- Kubernetes Secret resources are base64-encoded but not encrypted at rest by default — cluster-level encryption at rest must be configured separately in production.
