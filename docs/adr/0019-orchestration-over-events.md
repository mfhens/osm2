# ADR-0019: Orchestration over Event-Driven Choreography

**Status**: Accepted
**Date**: 2025-07-01
**Deciders**: Architecture Team

## Context

osm2 processes VAT registrations, return filings, and payments as sequential, stateful workflows with explicit state machines and clear compensation logic. An OSS registration, for example, proceeds through states: PENDING → ACTIVE → SUSPENDED → DEREGISTERED. A VAT return proceeds through: DRAFT → SUBMITTED → ASSESSED → PAID. These are not fire-and-forget events; they are ordered workflows where each step depends on the outcome of the previous.

Event-driven choreography (services reacting to domain events published to a message broker) would distribute this workflow logic across multiple services, making the overall flow opaque, difficult to test, and hard to reason about for incident response. Error recovery and compensation logic would be scattered across consumer services.

## Decision

osm2 uses **orchestration** (synchronous REST calls with Resilience4j circuit breakers — ADR-0026) as the primary inter-service communication pattern. Choreography via a message broker is not introduced at this stage.

The orchestration model is:
- **Portal BFFs** (taxable-person-portal, authority-portal) orchestrate calls to backend services to compose responses for their respective frontends.
- **return-service** orchestrates calls to registration-service (to validate registrant status) and payment-service (to initiate payment obligations) as part of return submission.
- **No message broker** (Kafka, RabbitMQ, ActiveMQ) is introduced. The additional operational complexity of a broker is not justified by the current workload characteristics.

Genuinely asynchronous, non-critical tasks (e.g., late-return reminder notifications, report generation) are handled via Spring's `@Async` executor with a bounded thread pool. These tasks are best-effort; failure does not affect transactional outcomes.

## Consequences

**Positive**
- Simpler distributed tracing: a single trace spans the full orchestration chain (ADR-0024).
- Orchestration flow is visible in the calling service's code — no need to read multiple consumer implementations to understand the workflow.
- Compensation logic (e.g., rolling back a partial return submission) is co-located with the business logic that initiated the workflow.
- Eliminates broker operational overhead (deployment, monitoring, consumer group management, dead-letter queue handling).

**Negative**
- Tighter temporal coupling than choreography: the orchestrating service must wait for downstream responses (mitigated by Resilience4j timeouts and circuit breakers — ADR-0026).
- If return-service is unavailable, portal-initiated return submissions fail immediately — there is no queue to absorb the load (acceptable per current SLA requirements).
- Scaling the orchestrator (return-service, portal BFFs) becomes a bottleneck under peak load, rather than scaling individual consumer services independently.
- Future introduction of a message broker (e.g., if asynchronous member-state API integration is required) will require architectural revision.
