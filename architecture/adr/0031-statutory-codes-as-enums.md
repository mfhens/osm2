# ADR-0031: Statutory Codes Represented as Java Enums

**Status**: Accepted
**Date**: 2025-07-01
**Deciders**: Architecture Team

## Context

The EU OSS regulation (Council Directive 2006/112/EC, as amended by Directives 2017/2455/EU and 2019/1995/EU) defines fixed, legislatively mandated codes for scheme types, return periods, and member state identifiers. The Danish implementing legislation (Momslovens §§ 66-66u) uses the same code set. These codes are stable between legislative cycles and are not user-configurable.

Representing statutory codes as plain `String` fields invites typos, makes invalid states representable in the domain model, and prevents the compiler from catching illegal code values. It also complicates API validation (every endpoint must validate strings against a set of permitted values) and makes refactoring fragile.

## Decision

All statutory codes defined by EU OSS legislation are represented as Java enums in `osm2-common`. The following enums are defined:

| Enum | Values | Notes |
|---|---|---|
| `SchemeType` | `NON_EU`, `EU`, `IMPORT` | Maps to ML § 66a (Non-EU), § 66b (EU / Union), § 66c (Import) |
| `ReturnPeriod` | `QUARTERLY` (with `year` and `quarter` fields) | OSS returns are filed quarterly per Art. 364/369f/369s of Dir. 2006/112/EC |
| `MemberState` | ISO 3166-1 alpha-2 codes for all 27 EU member states | Enum carries the official English name and Danish name |
| `RegistrationStatus` | `PENDING`, `ACTIVE`, `SUSPENDED`, `DEREGISTERED` | Registration lifecycle states per ML § 66e |
| `VatRateType` | `STANDARD`, `REDUCED`, `SUPER_REDUCED`, `ZERO` | Per Dir. 2006/112/EC Annex III |

Each enum value carries:
- The official statutory code (e.g., `NON_EU("NON_EU", "Ikke-EU-ordningen")`)
- A Danish description for display purposes

Enums are serialised to/from their statutory code string in JSON (via `@JsonValue` and `@JsonCreator`) and in PostgreSQL (via Hibernate's `@Enumerated(EnumType.STRING)`).

**Enforcement**: ArchUnit tests in `osm2-common` verify that no `String`-typed field with a name matching a statutory concept (e.g., `schemeType`, `memberState`, `registrationStatus`) exists in any entity or DTO class across the codebase. Violations are treated as build failures.

## Consequences

**Positive**
- Compiler-enforced valid states: an invalid scheme type or member state code is a compile error, not a runtime validation error.
- Refactoring is safe: IDEs can find all usages of `MemberState.DE` without risk of missing a string literal.
- ArchUnit prevents regression: a developer cannot accidentally introduce a `String schemeType` field without a build failure.
- JSON and database serialisation is consistent and automatic.

**Negative**
- Any future legislative change to scheme codes (e.g., a new scheme type, a member state leaving the EU) requires a code change, test update, and release. This is acceptable — legislative changes are rare, planned well in advance, and require coordinated system changes regardless.
- The `MemberState` enum with 27 values is verbose. It must be kept in sync with the current EU member state list (e.g., post-Brexit, the UK was removed).
- Hibernate `EnumType.STRING` stores the enum name, not the ordinal. Renaming an enum constant is a database migration event (Flyway `UPDATE` script required).
