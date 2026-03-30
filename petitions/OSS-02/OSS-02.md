# OSS-02 — Registrering og afmeldelse: Ikke-EU-ordning og EU-ordning

**Petition ID:** OSS-02  
**Status:** Draft  
**Created:** 2026-03-30  
**System:** osm2 — One Stop Moms (OSS) VAT system, Skatteforvaltningen  
**Legal basis:** ML §§ 66a–66j; Momsbekendtgørelsen §§ 115–119; Momsforordningen artikler 57d–58c; Momssystemdirektivet artikler 359–369j; Direktiv 2017/2455; Direktiv 2019/1995  

---

## 1. Summary

Implement the complete registration lifecycle for taxable persons (afgiftspligtige personer) under the Non-EU scheme (Ikke-EU-ordningen) and the EU scheme (EU-ordningen). The lifecycle covers: electronic registration with Denmark as identification member state, effective-date calculation, collection and validation of identification information, VAT number assignment within 8 days, maintenance of registration data including change notifications, voluntary deregistration (ML § 66i), and forced exclusion/deregistration (ML § 66j).

The EU scheme additionally enforces a binding rule: once a taxable person designates Denmark as identification member state under cases (b) or (c) of ML § 66d stk. 1, that choice binds for the current calendar year and the 2 following calendar years.

---

## 2. Context and Motivation

osm2 implements the EU One-Stop-Shop (OSS) VAT special arrangements as second-generation system replacing the prior implementation. The Non-EU scheme and EU scheme each require a complete, state-machine-governed registration lifecycle. Errors in registration logic — particularly the EU binding rule and the forced-exclusion timing — carry cross-border legal risk: if Denmark (as identification member state) fails to enforce exclusion criteria, other EU member states' VAT revenues are impacted.

OSS-02 establishes the foundational registration domain. Without correct registrations, downstream petition work (return filing OSS-04, payment OSS-05, cross-border data exchange) cannot proceed.

---

## 3. Actors

| Actor | Description |
|---|---|
| **Taxable person** | Legal or natural person registering under Non-EU or EU scheme |
| **Skatteforvaltningen** | Danish Tax Administration; acts as identification member state |
| **Registration system** | osm2 electronic registration and identification database |
| **Other EU member states** | Consume member states; notified of registration status via EU data exchange |

---

## 4. Functional Requirements

### 4.1 Electronic Registration — Non-EU Scheme (ML § 66b stk. 2)

**FR-OSS-02-001:** The system SHALL accept an electronic registration notification from a taxable person who declares they have no fixed establishment in any EU member state.  
**FR-OSS-02-002:** The system SHALL reject registration if the taxable person cannot substantiate the declaration of no EU fixed establishment (ML § 66a; Momsbekendtgørelsen § 115).  
**FR-OSS-02-003:** Registration under the Non-EU scheme SHALL be effective from the **first day of the calendar quarter** following the date of notification (ML § 66b stk. 2).  
**FR-OSS-02-004:** If the taxable person makes their first eligible delivery before submitting the registration notification, the effective date SHALL instead be the **date of that first delivery**, provided the notification is submitted no later than the **10th day of the month following** the first delivery (ML § 66b stk. 3; Momsforordningen artikel 57d).  
**FR-OSS-02-005:** If the notification is submitted after the 10th-day deadline in FR-OSS-02-004, the normal first-of-quarter rule in FR-OSS-02-003 SHALL apply.

### 4.2 Electronic Registration — EU Scheme (ML § 66e stk. 1–2)

**FR-OSS-02-006:** The system SHALL accept an electronic registration notification from a taxable person eligible under ML § 66e stk. 1 (intra-EU distance sales of goods, deemed supplier under ML § 4c stk. 2 with same-country dispatch-and-delivery, or services to non-taxable persons where taxable person is not established in consumption member state).  
**FR-OSS-02-007:** Registration under the EU scheme SHALL be effective from the **first day of the calendar quarter** following notification (ML § 66e stk. 2).  
**FR-OSS-02-008:** If the taxable person makes their first eligible delivery before submitting the notification, the effective date SHALL be the **date of first delivery**, provided notification is submitted no later than the **10th day of the month following** first delivery (ML § 66e stk. 2; Momsforordningen artikel 57d).

### 4.3 Identification Information — Non-EU Scheme (Momsbekendtgørelsen § 116 stk. 1)

**FR-OSS-02-009:** The registration SHALL collect the following mandatory identification fields for Non-EU scheme applicants:
1. Full name, including any trading names different from the legal name
2. Home country and home-country tax registration number
3. Postal address
4. Email address
5. Contact person for the scheme
6. Telephone number
7. Bank account details
8. Electronic websites (URLs)
9. Details of any current or prior registrations under the Import scheme, EU scheme, or Non-EU scheme in any member state

### 4.4 Identification Information — EU Scheme (Momsbekendtgørelsen § 117 stk. 1–3)

**FR-OSS-02-010:** The registration SHALL collect the following mandatory identification fields for EU scheme applicants:
1. Trading names different from the legal name (if any)
2. Home country, if not Denmark
3. Email address
4. Contact person for the scheme
5. Telephone number
6. Bank account details
7. Electronic websites (URLs, if any)
8. Details of any current or prior registrations under the Import scheme, EU scheme, or Non-EU scheme in any member state
9. VAT or tax registration numbers issued by other EU member states for fixed establishments, with corresponding trading names and postal addresses
10. VAT or tax registration numbers in EU member states from which goods are dispatched or transported, with corresponding trading names and postal addresses
11. VAT numbers issued by other member states as a non-established taxable person

**FR-OSS-02-011:** The EU scheme applicant SHALL additionally declare whether they are:
- An electronic interface under ML § 4c stk. 2
- A joint registration under ML § 47 stk. 4

**FR-OSS-02-012:** If the EU scheme applicant is not established in the EU, they SHALL declare this fact to Skatteforvaltningen (Momsbekendtgørelsen § 117 stk. 3).

### 4.5 VAT Number Assignment — Non-EU Scheme (Momsbekendtgørelsen § 116 stk. 3; Momssystemdirektivet artikel 362)

**FR-OSS-02-013:** Within **8 calendar days** of receiving complete identification information, the system SHALL assign a unique individual VAT registration number to the Non-EU scheme applicant and communicate it electronically.  
**FR-OSS-02-014:** If assignment cannot be completed within 8 days, the system SHALL notify the applicant within the 8-day window of the expected assignment date.  
**FR-OSS-02-015:** The VAT number assigned under the Non-EU scheme SHALL NOT be usable for any other VAT purpose outside that scheme (Momssystemdirektivet artikel 362).

### 4.6 VAT Number Assignment — EU Scheme (Momsbekendtgørelsen § 117 stk. 5–7; Momssystemdirektivet artikel 369d)

**FR-OSS-02-016:** If the EU scheme applicant is already VAT-registered in Denmark under ordinary rules, they SHALL use their existing Danish VAT number for the EU scheme; no new number is assigned.  
**FR-OSS-02-017:** If the EU scheme applicant is not already VAT-registered in Denmark, the system SHALL assign a new individual VAT number within **8 calendar days** and communicate it electronically.  
**FR-OSS-02-018:** If the applicant later ceases ordinary Danish VAT registration, the system SHALL automatically transition them to the EU-scheme VAT number issued under § 117 stk. 5, unless they are simultaneously deregistering from the EU scheme.  
**FR-OSS-02-019:** If assignment cannot be completed within 8 days, the system SHALL notify the applicant within the 8-day window of the expected assignment date.

### 4.7 EU Scheme Binding Rule (ML § 66d stk. 2; Momssystemdirektivet artikel 369a)

**FR-OSS-02-020:** When a taxable person selects Denmark as identification member state under ML § 66d stk. 1 nr. 2 (cases b or c: multiple fixed establishments or dispatch from multiple member states), that choice SHALL be binding for the **current calendar year and the 2 following calendar years**.  
**FR-OSS-02-021:** The system SHALL prevent the taxable person from changing identification member state away from Denmark during the binding period, except when a change of home establishment or fixed establishment means Denmark no longer qualifies as identification member state (in which case change is permitted and the binding period restarts in the new member state).  
**FR-OSS-02-022:** The system SHALL store and expose the binding period end date for each EU-scheme registration.

### 4.8 Registration Changes (Momsbekendtgørelsen §§ 116 stk. 2, 117 stk. 4; Momsforordningen artikel 57h)

**FR-OSS-02-023:** The system SHALL accept notifications of changes to any identification field submitted by the taxable person.  
**FR-OSS-02-024:** Change notifications SHALL be submitted no later than the **10th day of the month** following the change.  
**FR-OSS-02-025:** The system SHALL accept notification of business cessation or change that removes eligibility for the scheme; this notification is also due by the 10th day of the month following cessation/change (ML § 66b stk. 6, ML § 66e stk. 5).  
**FR-OSS-02-026:** If an EU scheme taxable person no longer satisfies the criteria for Denmark to be their identification member state (article 369a), they SHALL notify both Denmark and the new identification member state by the **10th day of the month** after the change, providing full registration information to the new member state (Momsforordningen artikel 57f, 57h stk. 2).

### 4.9 Voluntary Deregistration (ML § 66i; Momsforordningen artikel 57g stk. 1)

**FR-OSS-02-027:** A taxable person registered under the Non-EU or EU scheme MAY voluntarily deregister at any time, even if they continue to make eligible supplies.  
**FR-OSS-02-028:** The deregistration notification SHALL be submitted to Skatteforvaltningen at least **15 days before the end of the calendar quarter** preceding the quarter from which deregistration is to take effect.  
**FR-OSS-02-029:** Voluntary deregistration SHALL be effective from the **first day of the following calendar quarter**.  
**FR-OSS-02-030:** Voluntary deregistration SHALL NOT impose any re-entry penalty; the taxable person may re-register in any subsequent quarter (penalty abolished by lov nr. 810/2020 and Momsforordning 2019/2026).

### 4.10 Forced Exclusion and Deregistration (ML § 66j; Momsforordningen artikel 58)

**FR-OSS-02-031:** The system SHALL support forced exclusion of a taxable person from the Non-EU or EU scheme when any of the following criteria is met (ML § 66j stk. 1):
1. The taxable person notifies that they no longer make eligible supplies under the scheme
2. It can otherwise be assumed that the taxable person's eligible activities have ceased (presumed cessation: no supplies reported in any consumption member state for 2 consecutive years)
3. The taxable person no longer satisfies the conditions for using the scheme
4. The taxable person persistently fails to comply with the rules of the scheme

**FR-OSS-02-032:** Only Skatteforvaltningen, acting as identification member state, SHALL have authority to exclude a taxable person from the scheme. The decision SHALL be based on all available information including information from other member states (Momsforordningen artikel 58 stk. 1).  
**FR-OSS-02-033:** Forced exclusion SHALL be effective from the **first day of the calendar quarter** following the day the exclusion decision is sent electronically to the taxable person.  
**FR-OSS-02-034:** If the forced exclusion is caused by a change of home establishment, fixed establishment, or dispatch location that removes Denmark's status as identification member state, the exclusion SHALL be effective from the **date of that change** (Momsforordningen artikel 58 stk. 2).  
**FR-OSS-02-035:** A taxable person excluded for criterion 4 (persistent non-compliance) SHALL be subject to a **2-year exclusion penalty** during which re-registration is prohibited (ML § 66j).  
**FR-OSS-02-036:** After the exclusion effective date, the taxable person SHALL settle VAT obligations on eligible supplies directly with the consumption member states' tax authorities (Momsforordningen artikel 58c).

### 4.11 Scheme Switching

**FR-OSS-02-037:** When a taxable person moves their home establishment or fixed establishment such that they no longer qualify for the EU scheme but now qualify for the Non-EU scheme (or vice versa), the system SHALL support immediate scheme switching: exclusion from the prior scheme becomes effective on the date of the change, and registration in the new scheme also becomes effective on that date (applying the early-delivery rule from FR-OSS-02-004/FR-OSS-02-008).

### 4.12 Transitional Provision

**FR-OSS-02-038:** Taxable persons who were registered in the Non-EU or EU scheme before 1 July 2021 and have not updated their identification information under the new rules SHALL be required to submit updated identification information by **1 April 2022**. The system SHALL enforce this deadline and flag overdue registrations.

---

## 5. Non-Functional Requirements

*None stated by requestor for this petition. Downstream architecture may introduce performance, availability, and security constraints.*

---

## 6. Constraints and Assumptions

- Denmark is the identification member state for all registrations in scope.
- The Import scheme (Importordningen, ML §§ 66m–66u) is **out of scope** for this petition; it is addressed in OSS-03.
- The system is the authoritative registration record; cross-border data distribution to other EU member states is handled by the EU data exchange layer (separate concern).
- "Calendar quarter" means: Q1 = 1 Jan–31 Mar, Q2 = 1 Apr–30 Jun, Q3 = 1 Jul–30 Sep, Q4 = 1 Oct–31 Dec.
- "Day 10 of the following month" means the 10th calendar day of the month after the triggering event.
- The 8-day VAT number assignment window is counted in calendar days from receipt of complete information.
- The binding rule (FR-OSS-02-020) applies only to selection cases (b) and (c) under ML § 66d stk. 1; case (a) (home establishment in Denmark) does not require a binding commitment in the same way.

---

## 7. Out of Scope

- Import scheme (Importordningen) registration lifecycle → OSS-03
- VAT return (angivelse) filing → OSS-04
- Payment → OSS-05
- Cross-border EU data exchange protocols and message formats → separate concern
- Ordinary Danish VAT registration (momslovens regler) → separate system
- Refund/deduction mechanics (ML §§ 66c, 66f) → separate petition
- Appeals process against exclusion decisions → separate concern

---

## 8. Key Risks

| Risk | Description | Mitigation |
|---|---|---|
| EU binding rule underspecification | EU scheme binding (current year + 2) is complex; wrong implementation could allow illegal identification state changes | State-machine with explicit binding-period dates; enforced in FR-OSS-02-020–022 |
| Effective-date boundary conditions | Quarter-start and 10-day deadline calculations must be exact | Explicit date-arithmetic tests in Gherkin scenarios |
| Forced-exclusion timing | Two different effective-date rules (first of quarter vs. date of change) for forced exclusion | Separate scenarios for each criterion |
| Scheme switching | Overlapping effective dates on exclusion and re-entry | FR-OSS-02-037 explicitly handles the switch |
| Transitional data quality | Pre-July-2021 registrants may have incomplete records | FR-OSS-02-038 enforces April 2022 update deadline |

---

## 9. References

| Reference | Location |
|---|---|
| D.A.16.3.2 Ikke-EU-ordningen | `docs/references/DA16.3.2-ikke-eu-ordningen.md` |
| D.A.16.3.3 EU-ordningen | `docs/references/DA16.3.3-eu-ordningen.md` |
| D.A.16.3.5 Anmeldelse og afmeldelse | `docs/references/DA16.3.5-anmeldelse-og-afmeldelse.md` |
| D.A.16.3.5.1 Anmeldelse og virkningstidspunkt | `docs/references/DA16.3.5.1-anmeldelse-og-virkningstidspunkt.md` |
| D.A.16.3.5.2 Oplysninger i anmeldelsen | `docs/references/DA16.3.5.2-oplysninger-i-anmeldelsen.md` |
| D.A.16.3.5.3 Tildeling af momsnummer | `docs/references/DA16.3.5.3-tildeling-af-momsnummer.md` |
| D.A.16.3.5.4 Anmeldelse fra 1. april 2021 | `docs/references/DA16.3.5.4-anmeldelse-fra-1-oktober-2020.md` |
| D.A.16.3.5.5 Ændringer | `docs/references/DA16.3.5.5-aendringer.md` |
| D.A.16.3.5.6 Tvangsafmeldelse og udelukkelse | `docs/references/DA16.3.5.6-tvangsafmeldelse-og-udelukkelse.md` |
| D.A.16.3.5.7 Frivillig afmeldelse | `docs/references/DA16.3.5.7-frivillig-afmeldelse.md` |
| ML §§ 66a–66j | Momslov (lov nr. 209/2024) |
| Momsbekendtgørelsen §§ 115–119 | Bekendtgørelse nr. 1435/2023 |
| Momsforordningen artikler 57d–58c | Gennemførelsesforordning (EU) 2019/2026 |
| Direktiv 2017/2455 | EU OSS reform directive |
| Direktiv 2019/1995 | EU OSS extension directive |
