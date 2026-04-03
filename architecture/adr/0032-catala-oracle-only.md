# ADR-0032: Catala as Formal Legal Oracle (CI-Gated, Not Runtime Dependency)

**Status**: Accepted  
**Date**: 2025-07-01  
**Amended**: 2026-04-03  
**Deciders**: Architecture Team

## Context

The legal rules governing OSS scheme eligibility (Momslovens §§ 66-66u) are complex statutory logic: conditions on registration thresholds, scheme exclusion criteria, mandatory registration periods, and cross-scheme interactions. Statutory logic of this nature is notoriously difficult to implement correctly and equally difficult to verify — a discrepancy between the code and the law may not surface until a tax authority audit.

Catala is a domain-specific language designed for formally encoding law, developed by Inria and the French Ministry of Finance. It enables legal experts to write executable specifications that can be formally verified against the statutory text. However, the Catala compiler targets OCaml and C runtimes; JVM output is not production-hardened. Using Catala as a runtime dependency on the JVM would introduce an unsupported execution path.

Drools (the actual runtime rules engine used in `osm2-scheme-service`) provides the performance and JVM integration required for production, but Drools rules are not human-readable by legal experts.

## Decision

Catala (`.catala_da` files — Danish language variant) is used as a **formal oracle layer** for osm2. It is **not** a runtime dependency.

Catala is **accepted** as a **first-class compliance artefact**: specifications are version-controlled, reviewed, and **validated online** in the continuous-integration pipeline (compiler `typecheck` and, where present, Catala test scopes). Failing Catala validation **blocks** merges/releases for affected components, on the same footing as other mandatory checks.

**Scope**: Catala encodes ML §§ 66-66u scheme eligibility rules. Files live under `src/main/catala/` within `osm2-scheme-service` (and tests under `src/main/catala/tests/`). The Catala files are the authoritative legal specification for scheme eligibility logic. Additional petitions may add `.catala_da` files in other services (e.g. registration); those files follow the same oracle discipline and are included in CI validation when present.

**Validation process**:
1. The Catala compiler (`catala`) runs in **CI** against every committed `.catala_da` file (e.g. `catala typecheck --language en --no-stdlib` on each module and its tests). This is an **online**, automated gate — not a manual-only or release-only step.
2. A suite of eligibility test scenarios (Catala tests beside sources, and/or scenarios under `src/test/catala-scenarios/` where used) is run against both the Catala oracle and the Drools rules engine where the product compares outcomes.
3. Any discrepancy between Catala output and Drools output is a **blocking defect**: the Drools rules must be corrected to match the Catala specification.

**Change process**: On every legislative change to ML §§ 66-66u, the legal-technical team updates the Catala files first, then updates the Drools rules to match. **CI must stay green** with the updated Catala sources before the change is considered integrated.

**Authorship**: Catala files are maintained by the legal-technical team (legal experts with Catala training). Drools rules are maintained by the engineering team. The two artefacts are reviewed independently and cross-validated via the oracle process.

**Note on Maven**: The Catala toolchain remains **outside** the JVM (`mvn verify` does not invoke the Catala compiler). **Online** validation is provided by the **CI workflow** that installs or containers the Catala CLI. That split keeps the Java build standard while still enforcing Catala on every push/PR.

## Implementation Note (OSS-01)

OSS-01 delivered the first Drools runtime implementation of the Catala-specified rules. The file `osm2-scheme-service/src/main/resources/rules/scheme-classification.drl` contains 20 rules covering **FR-01 through FR-08** — the scheme eligibility logic for ML §§ 66-66u (Non-EU, EU/Union, and Import OSS schemes). This DRL is the authoritative runtime artefact that the Catala oracle validates.

The Catala oracle validation step (Catala compiler → eligibility scenario comparison) for FR-01 through FR-08 is a required gate before the scheme-service is promoted to a production release.

## Consequences

**Positive**
- Formal legal correctness verification: the Catala specification can be reviewed by legal experts who cannot read Drools DRL syntax, providing a human-readable, formally verifiable expression of the law.
- Catala files provide a legislative traceability artefact: each rule in Catala references the specific paragraph of Momsloven that it encodes.
- Any regression in Drools rule logic is caught by the Catala oracle comparison before release.
- **CI enforcement** gives continuous assurance that committed Catala sources typecheck (and tests pass), reducing drift between “paper” law and merged code.

**Negative**
- The repository depends on a **non-JVM** toolchain in CI (Catala CLI version must be pinned or imaged); build maintainers must keep that job fast and reliable.
- Maintaining two parallel representations of the same logic (Catala and Drools) is a synchronisation overhead. If the Catala files fall out of date, the oracle loses its value.
- Requires at least one team member with expertise in both Catala syntax and Danish VAT law — a rare skill combination. Knowledge transfer and documentation are critical.
- Catala's `.catala_da` Danish variant has a smaller community and tooling ecosystem than the French (`.catala_fr`) variant. Community support for Danish-specific issues is limited.
