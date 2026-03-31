# ADR-0035: Defer SupplyType and EnrolledScheme Enums to osm2-common Until Shared

**Status**: Accepted
**Date**: 2025-07-14
**Deciders**: Architecture Team

## Context

ADR-0031 mandates that statutory codes are represented as Java enums in `osm2-common`. During OSS-01 implementation, two domain enums were required by `osm2-scheme-service`:

- **`SupplyType`** — distinguishes goods-based from services-based supply (relevant for EU-scheme threshold logic under ML § 66b).
- **`EnrolledScheme`** — identifies the OSS schemes a taxable person is already enrolled in at the time of the classification request (used for cross-scheme exclusion rules FR-04 through FR-08).

At the time of OSS-01, `osm2-common` contained only `DemoConstants.java`. No shared enum infrastructure existed and `osm2-scheme-service` was the first service to be implemented. Promoting these enums to `osm2-common` preemptively would require:

1. Adding a domain concept from `osm2-scheme-service` into the shared module before any other service needed it.
2. Accepting `osm2-common` as a coordination point for every enum refinement during active OSS-01 development.
3. Violating YAGNI: shared infrastructure should be extracted when two consumers exist, not speculatively in advance of a second consumer.

The ADR-0031 ArchUnit enforcement rule targets `String`-typed fields whose names match statutory concept names in entity and DTO classes. Nested enums inside a DTO are not caught by this rule in its current form.

## Decision

`SupplyType` and `EnrolledScheme` are defined as **nested enums inside `SchemeClassificationRequest`** (the input DTO in `osm2-scheme-service`) for the duration of OSS-01.

When a second service requires either enum (for example, `osm2-registration-service` validating supply type on registration submission), the enum **must** be promoted to `osm2-common` at that point, in full compliance with ADR-0031.

This ADR documents a **time-bounded, YAGNI-justified deferral** for the first-service case only. It does **not** modify or supersede ADR-0031. ADR-0031's mandate remains in force for all multi-consumer enums.

**Promotion trigger**: A pull request that introduces a second consumer of either enum is the mandatory trigger for the promotion refactor. The PR must not merge without the enum existing in `osm2-common`.

## Consequences

**Positive**

- No speculative `osm2-common` API surface during OSS-01 development. `osm2-scheme-service` remains self-contained and independently releasable during its initial implementation phase.
- Enum values are co-located with the DTO that uses them, making OSS-01 changes easier to review and trace.
- Promotion is triggered by observable evidence (a second consumer) rather than by anticipation, keeping `osm2-common` lean.

**Negative**

- Until promotion, `SupplyType` and `EnrolledScheme` are not importable by other services. A second service needing these enums must wait for or trigger the promotion PR, which adds a cross-team coordination step.
- The ADR-0031 ArchUnit rule does not currently cover nested enums in DTOs. A follow-up ArchUnit rule must be added when promotion occurs to enforce that future statutory enums reside in `osm2-common`.
- Risk of duplication: a developer could introduce equivalent enums in a second service before noticing the deferral. Code review and this ADR are the only guard against this until promotion.
