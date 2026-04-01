# OSS2 Domain Concept Catalogue

**Source:** Den juridiske vejledning 2026-1 — D.A.16 (Skatteforvaltningen)  
**Retrieved:** 2026-03-30  
**Model version:** 1.0  
**Status:** Draft

---

## Table of Contents

- [Schemes](#schemes)
- [Parties](#parties)
- [Member States](#member-states)
- [Registration](#registration)
- [Deregistration](#deregistration)
- [Supplies](#supplies)
- [Returns](#returns)
- [Payment](#payment)
- [Records](#records)
- [Implementation Gap Analysis](#implementation-gap-analysis)

---

## Schemes

### Særordning *(Special scheme — OSS)*

Abstract supertype for all three One Stop Shop schemes. Allows a taxable person to report and pay VAT for all EU consumption member states through a single identification member state.

**Legal basis:** ML §§ 66–66u; Momssystemdirektivet art. 358–369x  
**Implementation:** `SchemeType` enum (NON_EU / EU / IMPORT) ✅

---

### Ikke-EU-ordningen *(Non-Union scheme)*

For taxable persons with no establishment in the EU, supplying services to non-taxable persons (consumers) in EU member states.

| Attribute | Value |
|---|---|
| Eligible parties | Non-EU-established taxable persons |
| Supply types | Services to EU consumers |
| Return period | Quarter |
| Legal basis | ML §§ 66a–66c |

**Implementation:** `SchemeType.NON_EU` ✅

---

### EU-ordningen *(Union scheme)*

For EU-established taxable persons. Covers intra-EU distance sales of goods, B2C cross-border services, and platform-facilitated supplies.

| Attribute | Value |
|---|---|
| Eligible parties | EU-established taxable persons |
| Supply types | Services + intra-EU distance sales of goods |
| Return period | Quarter |
| Binding period | 2 years on choice of MSI |
| Legal basis | ML §§ 66d–66j |

**Implementation:** `SchemeType.EU`, `SchemeRegistration.bindingPeriodEnd` ✅

---

### Importordningen *(Import scheme)*

For B2C distance sales of goods imported from third territories (non-EU), with intrinsic value ≤ EUR 150. Excise goods excluded. Can be used directly or through an EU-established intermediary.

| Attribute | Value |
|---|---|
| Eligible parties | Any taxable person (or their intermediary) |
| Supply types | Distance sales of goods imported from third territories |
| Value threshold | ≤ EUR 150 (intrinsic value) |
| Return period | Month |
| Legal basis | ML §§ 66m–66t |

**Implementation:** `SchemeType.IMPORT` ✅

---

## Parties

### Afgiftspligtig person *(Taxable person)*

The person registering for an OSS scheme. Subtypes:
- **EU-etableret:** Has principal establishment or fixed establishment in at least one EU member state.
- **Ikke-EU-etableret:** No establishment anywhere in the EU.

**Implementation:** `Registrant` entity ✅  
`Registrant.homeCountry`, `identificationMemberState`, `vatNumber`, `status`

---

### Ikkeafgiftspligtig person *(Non-taxable person / Consumer)*

The recipient of supplies under OSS schemes — a private consumer established, resident or habitually residing in an EU member state.

**Implementation:** Out of scope for current services ❌

---

### Formidler *(Intermediary)*

An EU-established taxable person appointed by an Import scheme user to file returns and pay VAT on their behalf. Jointly and severally liable with the taxable person for VAT due.

**Legal basis:** ML § 66m; Momssystemdirektivet art. 369l  
**Implementation:** Not yet modelled. No `Intermediary` entity. ❌ **Gap — medium severity.**

---

## Member States

### Identifikationsmedlemsland *(Member State of identification — MSI)*

The EU member state where the taxable person registers for an OSS scheme and to which they report and remit all VAT. Only one MSI per scheme at any time.

**Implementation:** `Registrant.identificationMemberState` (ISO 3166-1 alpha-2) ✅

---

### Forbrugsmedlemsland *(Member State of consumption — MSC)*

The EU member state where a supply is deemed to take place under place-of-supply rules. The MSI forwards the corresponding VAT to each MSC.

**Implementation:** Captured per return line in VAT return — return service not yet implemented ⚠️

---

### Fast forretningssted *(Fixed establishment)*

A business establishment (not principal place) with sufficient permanent resources to receive and use services. Relevant for determining which MSI a taxable person may choose.

**Legal basis:** Momsforordningen art. 11  
**Implementation:** Data collected at registration but not stored as a separate entity. ⚠️ Low severity.

---

## Registration

### Anmeldelse *(Registration notification)*

The electronic filing initiating participation in an OSS scheme. Must include identification data and the desired start date.

**Timing rules:**
- Normal: effective from 1st day of quarter following the quarter of notification.
- Early-delivery exception: effective from date of first supply if this precedes the normal effective date (ML § 66b stk. 3).

**Implementation:** `RegistrationService.submitRegistration()` ✅  
Creates `SchemeRegistration` with `registrationStatus = PENDING`.

---

### Momsregistreringsnummer *(OSS VAT identification number)*

Assigned within 8 working days. Scheme-specific for Non-EU and Import schemes; existing Danish VAT number (or new individual number) for EU scheme.

**Implementation:** `Registrant.vatNumber`, `SchemeRegistration.vatNumber`, `SchemeRegistration.vatNumberFlag` ✅

---

### Bindingsperiode *(Binding period)*

EU scheme only. The taxable person is bound to their chosen MSI for 2 years (ML § 66d stk. 2).

**Implementation:** `SchemeRegistration.bindingPeriodEnd`, `SchemeRegistration.bindingRuleType` ✅

---

## Deregistration

### Frivillig afmeldelse *(Voluntary deregistration)*

Taxable person may cease at any time, regardless of whether they continue making eligible supplies.

| Scheme | Notice deadline | Effective date |
|---|---|---|
| Non-EU / EU | ≥15 days before end of quarter prior to cessation quarter | 1st day of following quarter |
| Import | ≥15 days before end of month prior to cessation month | 1st day of following month |

Note: Voluntary deregistration does **not** trigger an exclusion period (changed by Directive 2019/2026).

**Implementation:** `RegistrationService.deregisterRegistration()` ✅  
`SchemeRegistration.deregistrationEffectiveDate`, `deregistrationTimely`

---

### Tvangsafmeldelse / Udelukkelse *(Forced deregistration / Exclusion)*

Authority-initiated removal. Escalates to **exclusion** (most severe form) when persistent non-compliance is established.

**Exclusion criteria:**

| Criterion | Definition |
|---|---|
| `PERSISTENT_NON_COMPLIANCE` | ≥3 consecutive missed returns, OR ≥3 consecutive late payments, OR failure to comply with records obligations after a tvangsafmeldelse warning |
| `NO_LONGER_ELIGIBLE` | Taxable person no longer meets eligibility conditions |
| `FRAUD` | Fraud or abuse of the scheme |

**Implementation:** `ExclusionService.excludeRegistration()` ✅  
`ExclusionBan` entity + `SchemeRegistration` exclusion fields

---

### Udelukkelsesperiode *(Exclusion period)*

2-year re-registration ban, computed from the exclusion's `valid_to` (effective date, not decision date).

**Legal basis:** ML § 66d stk. 3; § 66b stk. 5; § 66n stk. 3  
**Implementation:** `ExclusionBan.banLiftedAt` ✅

---

## Supplies

### Fjernsalg af varer inden for EU *(Intra-EU distance sales of goods)*

EU scheme. Goods dispatched from one EU country to a consumer in another.  
**Implementation:** Not yet implemented — planned for `osm2-return-service` ❌ High severity.

---

### Fjernsalg af varer fra tredjelande *(Distance sales of goods from third territories)*

Import scheme. Goods shipped from outside EU to EU consumers. Value ≤ EUR 150; excise goods excluded.  
**Implementation:** Not yet implemented ❌ High severity.

---

### Elektronisk leverede ydelser *(Electronically supplied services)*

Non-EU / EU schemes. Highly automated services delivered via internet (software, streaming, apps, online games etc.).  
**Implementation:** Not yet implemented ❌

---

## Returns

### Afgiftsangivelse / Momsangivelse *(VAT return)*

Filed electronically to MSI per return period. Must always be filed (nil return required when no supplies). No rounding permitted. Per-MSC breakdown required. Corrections to prior returns included in current period return.

**Return period by scheme:**

| Scheme | Period | Filing deadline |
|---|---|---|
| Non-EU | Quarter | End of month following quarter |
| EU | Quarter | End of month following quarter |
| Import | Month | End of month following month |

**Implementation:** Not yet implemented — `osm2-return-service` ❌ **High severity.**

---

### Angivelsesperiode *(Return period / Tax period)*

The time window covered by a single VAT return. Quarter for Non-EU/EU; calendar month for Import.

**Implementation:** Not yet implemented ❌

---

## Payment

### Betaling *(VAT payment)*

Must accompany the return filing. Paid to MSI (in DKK when Denmark is MSI). Corrections reference the specific original return period — no netting across periods.

**Implementation:** Not yet implemented — `osm2-payment-service` ❌ **High severity.**

---

### Hæftelse *(Joint and several liability)*

Import scheme only. The taxable person and their intermediary are jointly and severally liable for VAT due.

**Implementation:** Requires Intermediary entity and payment service ❌

---

## Records

### Regnskab *(Accounting records)*

The taxable person (or intermediary) must maintain transaction-level records for all OSS supplies. Must be sufficiently detailed for MSC tax authorities to verify the VAT return. Available electronically on request within 20 days. 10-year retention.

**Implementation:** Not yet implemented — `osm2-records-service` ❌ **High severity.**

---

## Implementation Gap Analysis

| Concept | Status | Severity | Owner service | Notes |
|---|---|---|---|---|
| Særordning (SchemeType enum) | ✅ Implemented | — | registration | — |
| AfgiftspligtigPerson (Registrant) | ✅ Implemented | — | registration | — |
| Anmeldelse / SchemeRegistration | ✅ Implemented | — | registration | — |
| Momsregistreringsnummer | ✅ Implemented | — | registration | — |
| Identifikationsmedlemsland | ✅ Implemented | — | registration | — |
| Bindingsperiode | ✅ Implemented | — | registration | EU scheme only |
| FrivilligAfmeldelse | ✅ Implemented | — | registration | — |
| Udelukkelse / ExclusionBan | ✅ Implemented | — | registration | — |
| Udelukkelsesperiode | ✅ Implemented | — | registration | — |
| Formidler | ❌ Not implemented | Medium | registration | No Intermediary entity; Import scheme affected |
| Forbrugsmedlemsland (return lines) | ⚠️ Partial | Medium | return | Entity concept exists; return-line model missing |
| Fast forretningssted (entity) | ⚠️ Partial | Low | registration | Data collected at registration; no entity |
| Afgiftsangivelse | ❌ Not implemented | **High** | return | `osm2-return-service` pending |
| Angivelsesperiode | ❌ Not implemented | **High** | return | Depends on return service |
| Betaling | ❌ Not implemented | **High** | payment | `osm2-payment-service` pending |
| Hæftelse | ❌ Not implemented | **High** | payment | Depends on Formidler + payment service |
| Regnskab | ❌ Not implemented | **High** | records | `osm2-records-service` pending |
| Opbevaringsperiode | ❌ Not implemented | **High** | records | 10-year retention rule |
| Supply type entities | ❌ Not implemented | High | return | Fjernsalg, elektroniske ydelser |
| IkkeafgiftspligtigPerson | ❌ Out of scope | Low | — | Consumer identity not stored |
