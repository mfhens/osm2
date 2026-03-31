# ADR-0026: Resilience4j for Circuit Breaking and Retry

**Status**: Accepted
**Date**: 2025-07-01
**Deciders**: Architecture Team

## Context

osm2 services make synchronous calls to each other (orchestration — ADR-0019) and to external systems: Keycloak (token validation), immudb (payment ledger writes), the ECB exchange rate feed (daily FX rates), and member-state tax authority APIs (future). Without circuit breakers, a slow or unavailable downstream dependency causes the calling service's thread pool to exhaust, cascading the failure upwards. A single slow external API call can bring down the entire portal response chain.

## Decision

**Resilience4j 2.4.0** is the circuit breaker and retry library for all inter-service and external API calls in osm2.

Every `WebClient` (reactive) or `RestClient` (imperative) call to a downstream service or external API is decorated with:
- A `@CircuitBreaker` — opens after a configurable failure rate threshold (default: 50% over a sliding window of 10 calls); half-opens after a wait duration (default: 30 seconds).
- A `@Retry` — retries idempotent calls up to 3 times with exponential backoff (initial interval 500 ms, multiplier 2).

Non-idempotent calls (e.g., payment write to immudb) use circuit breakers without retry to avoid duplicate writes.

Every protected call has a **fallback method** that either returns a safe default (e.g., a cached FX rate for the ECB feed) or throws a domain-specific exception that the orchestrator can handle (e.g., `RegistrationServiceUnavailableException`).

Configuration is via Spring Boot auto-configuration (`resilience4j.circuitbreaker.*` and `resilience4j.retry.*` in `application.yml`). Per-service configuration is externalised to environment-specific `application-{env}.yml` files.

Circuit breaker state (CLOSED, OPEN, HALF_OPEN) and metrics (failure rate, call volume, wait duration) are exposed via Spring Boot Actuator (`/actuator/circuitbreakers`) and scraped by Prometheus (ADR-0024).

## Consequences

**Positive**
- Cascading failure prevention: an open circuit breaker fails fast, preventing thread pool exhaustion in the calling service.
- Configurable retry with exponential backoff handles transient failures (e.g., brief Keycloak unavailability) without manual intervention.
- Spring Boot auto-configuration reduces boilerplate; Resilience4j integrates natively with Spring AOP.
- Circuit breaker state is observable via Prometheus and Grafana, enabling proactive alerting before cascading failures occur.

**Negative**
- Fallback logic must be defined for every protected call — omitting a fallback results in the default behaviour (re-throwing the exception), which may not be appropriate.
- Retry on non-idempotent calls (if accidentally applied) can cause duplicate mutations. This requires careful annotation discipline.
- Circuit breaker tuning (failure rate threshold, wait duration, sliding window size) requires load testing data for each downstream dependency. Initial defaults may not be appropriate for all environments.
- Circuit breaker state is per-instance (in-memory). In a multi-replica deployment, each pod has its own circuit breaker state — a dependency may be open on some pods and closed on others.
