# ADR-0024: OpenTelemetry and Grafana Observability Stack

**Status**: Accepted
**Date**: 2025-07-01
**Deciders**: Architecture Team

## Context

With 8 independent services, diagnosing production incidents requires correlating log lines, metrics, and request traces across service boundaries. A single HTTP request initiated at the taxable-person-portal may span 4 or more backend services before completing. Without distributed tracing, root-cause analysis is a manual, error-prone process of correlating timestamps across service logs. Without centralised metrics, capacity planning and SLA monitoring require bespoke tooling per service.

## Decision

All osm2 services export telemetry to an **OpenTelemetry Collector** via the OTLP protocol. The OTel Collector fans out to three backends:

| Signal | Backend | Port |
|---|---|---|
| Distributed traces | Grafana Tempo | 3200 |
| Metrics | Prometheus | 9090 |
| Logs | Grafana Loki | 3100 |

**Grafana** (port 3000) is the single observability UI, with pre-configured datasources pointing to Tempo, Prometheus, and Loki. Trace-to-log and trace-to-metric correlation is enabled via exemplars.

**Configuration per service:**
- `OTEL_SERVICE_NAME` — set to the Maven `artifactId` of the service.
- `OTEL_EXPORTER_OTLP_ENDPOINT` — injected from Kubernetes ConfigMap / Docker Compose environment.
- `OTEL_RESOURCE_ATTRIBUTES` — includes `deployment.environment` for environment-based filtering in Grafana.

**Structured logging**: All services use `net.logstash.logback:logstash-logback-encoder` to produce JSON log output. Promtail scrapes container logs and ships them to Loki. Every log line carries the OpenTelemetry `trace_id` and `span_id` fields, enabling direct navigation from a Loki log line to the corresponding Tempo trace.

The full observability stack (OTel Collector, Tempo, Prometheus, Loki, Grafana, Promtail) is defined in `docker-compose.observability.yml` for local development and integration testing.

## Consequences

**Positive**
- End-to-end trace correlation across all 8 services from a single Grafana UI.
- Log lines are directly linkable to traces: from a Loki log entry, a developer can jump to the full distributed trace in Tempo.
- Prometheus metrics enable automated alerting (Grafana Alerting) and SLA dashboards.
- OTel is vendor-neutral: the backend can be swapped (e.g., Tempo → Jaeger, Loki → Elasticsearch) without changing application code.

**Negative**
- The OTel Collector is a critical dependency for observability: if it is unavailable, traces and metrics are lost (services continue to function, but observability is degraded).
- `logstash-logback-encoder` must be on every service classpath; human-readable log output in development requires a Logback filter or profile-specific configuration.
- The observability stack adds 5+ containers to the local Docker Compose environment, increasing memory requirements for developer workstations.
- Prometheus scrape configuration must be updated when new services are added.
