# ADR-0032: Catala as Formal Legal Oracle (Not Runtime Dependency)

**Status**: Accepted
**Date**: 2025-07-01
**Deciders**: Architecture Team

## Context

The legal rules governing OSS scheme eligibility (Momslovens §§ 66-66u) are complex statutory logic: conditions on registration thresholds, scheme exclusion criteria, mandatory registration periods, and cross-scheme interactions. Statutory logic of this nature is notoriously difficult to implement correctly and equally difficult to verify — a discrepancy between the code and the law may not surface until a tax authority audit.

Catala is a domain-specific language designed for formally encoding law, developed by Inria and the French Ministry of Finance. It enables legal experts to write executable specifications that can be formally verified against the statutory text. However, the Catala compiler targets OCaml and C runtimes; JVM output is not production-hardened. Using Catala as a runtime dependency on the JVM would introduce an unsupported execution path.

Drools (the actual runtime rules engine used in `osm2-scheme-service`) provides the performance and JVM integration required for production, but Drools rules are not human-readable by legal experts.

## Decision

Catala (`.catala_da` files — Danish language variant) is used as a **formal oracle layer** for osm2. It is not a runtime dependency.

**Scope**: Catala encodes ML §§ 66-66u scheme eligibility rules. Files live in `src/main/catala/` within `osm2-scheme-service`. The Catala files are the authoritative legal specification for scheme eligibility logic.

**Validation process**:
1. The Catala compiler (`catala`) is used offline (not in the Maven build) to compile the Catala specification to an executable test harness.
2. A suite of eligibility test scenarios (defined in `src/test/catala-scenarios/`) is run against both the Catala oracle and the Drools rules engine.
3. Any discrepancy between Catala output and Drools output is a **blocking defect**: the Drools rules must be corrected to match the Catala specification.

**Change process**: On every legislative change to ML §§ 66-66u, the legal-technical team updates the Catala files first, then updates the Drools rules to match. The Catala-vs-Drools validation runs as part of the release acceptance gate.

**Authorship**: Catala files are maintained by the legal-technical team (legal experts with Catala training). Drools rules are maintained by the engineering team. The two artefacts are reviewed independently and cross-validated via the oracle process.

## Implementation Note (OSS-01)

OSS-01 delivered the first Drools runtime implementation of the Catala-specified rules. The file `osm2-scheme-service/src/main/resources/rules/scheme-classification.drl` contains 20 rules covering **FR-01 through FR-08** — the scheme eligibility logic for ML §§ 66-66u (Non-EU, EU/Union, and Import OSS schemes). This DRL is the authoritative runtime artefact that the Catala oracle validates.

The Catala oracle validation step (Catala compiler → eligibility scenario comparison) for FR-01 through FR-08 is a required gate before the scheme-service is promoted to a production release.

## Consequences

**Positive**
- Formal legal correctness verification: the Catala specification can be reviewed by legal experts who cannot read Drools DRL syntax, providing a human-readable, formally verifiable expression of the law.
- Catala files provide a legislative traceability artefact: each rule in Catala references the specific paragraph of Momsloven that it encodes.
- Any regression in Drools rule logic is caught by the Catala oracle comparison before release.

**Negative**
- The Catala compiler is not integrated into the Maven build (`mvn verify` does not run Catala validation). Catala validation is a manual step in the release acceptance gate. Automation requires a CI pipeline stage with a Catala compiler Docker image.
- Maintaining two parallel representations of the same logic (Catala and Drools) is a synchronisation overhead. If the Catala files fall out of date, the oracle loses its value.
- Requires at least one team member with expertise in both Catala syntax and Danish VAT law — a rare skill combination. Knowledge transfer and documentation are critical.
- Catala's `.catala_da` Danish variant has a smaller community and tooling ecosystem than the French (`.catala_fr`) variant. Community support for Danish-specific issues is limited.
