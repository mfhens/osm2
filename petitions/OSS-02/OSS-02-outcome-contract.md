# OSS-02 — Outcome Contract: Registrering og afmeldelse: Ikke-EU-ordning og EU-ordning

**Petition ID:** OSS-02  
**Status:** Draft  
**Created:** 2026-03-30  

---

## 1. Definition of Done

The implementation of OSS-02 is **done** when:

1. All Gherkin scenarios in `OSS-02.feature` pass against the deployed system.
2. All functional requirements FR-OSS-02-001 through FR-OSS-02-038 are covered by at least one passing automated test.
3. No acceptance criterion in section 2 is in a failing or untested state.
4. The registration state machine is documented and its transitions match the legal rules in ML §§ 66b, 66d, 66e, 66i, 66j.
5. No manual workaround exists for any lifecycle step; all registration events are processed by the system.

---

## 2. Acceptance Criteria

### AC-OSS-02-01 — Non-EU Scheme: Electronic Registration Accepted

**Given** a taxable person with no EU fixed establishment  
**When** they submit a valid electronic registration notification for the Non-EU scheme  
**Then** the system records the registration with status PENDING_VAT_NUMBER  
**And** the declared desired start date is stored  
**And** the system does not reject the registration on grounds of non-EU establishment  

**Failure condition:** System rejects a valid registration or assigns wrong status.

---

### AC-OSS-02-02 — Effective Date: Normal Quarter Rule

**Given** a taxable person submits a Non-EU or EU scheme registration notification on date D  
**When** no prior eligible delivery has been made before D  
**Then** the registration effective date is the **first day of the calendar quarter following D**  

**Failure condition:** Effective date is set to D itself, or to any date other than the first day of the next quarter.

---

### AC-OSS-02-03 — Effective Date: Early Delivery Exception

**Given** a taxable person makes their first eligible delivery on date F  
**And** they submit the registration notification no later than the 10th day of the month following F  
**When** the system processes the registration  
**Then** the registration effective date is **F** (the date of the first delivery)  

**Failure condition:** System sets effective date to start of quarter rather than first delivery date.

---

### AC-OSS-02-04 — Effective Date: Late Notification After Early Delivery Forfeits Exception

**Given** a taxable person makes their first eligible delivery on date F  
**And** they submit the registration notification after the 10th day of the month following F  
**When** the system processes the registration  
**Then** the effective date is the **first day of the calendar quarter following** the notification date  
**And** the early-delivery exception does NOT apply  

**Failure condition:** System backdates the effective date to the first delivery date.

---

### AC-OSS-02-05 — Identification Information: Non-EU Scheme Completeness

**Given** a Non-EU scheme registration submission  
**When** any of the 9 mandatory identification fields (name, home country + tax number, postal address, email, contact person, phone, bank details, websites, prior registrations) is missing  
**Then** the system rejects the submission with a field-level validation error identifying the missing field(s)  

**Failure condition:** System accepts an incomplete submission or produces a generic error without field identification.

---

### AC-OSS-02-06 — Identification Information: EU Scheme Completeness

**Given** an EU scheme registration submission  
**When** any mandatory field from §117 stk. 1 items 1–12 (as applicable) is missing, or the electronic-interface / joint-registration declaration is absent  
**Then** the system rejects the submission with a field-level validation error  

**Failure condition:** System accepts an incomplete EU scheme registration.

---

### AC-OSS-02-07 — VAT Number Assignment: Non-EU Scheme Within 8 Days

**Given** a complete Non-EU scheme registration is received on date R  
**When** 8 calendar days have not yet elapsed since R  
**Then** the system assigns a unique VAT registration number and communicates it electronically to the taxable person  
**And** the number is flagged as Non-EU scheme only (not usable for other VAT purposes)  

**Failure condition:** Number is assigned after day 8 without a prior notification, or the number is not flagged as scheme-specific.

---

### AC-OSS-02-08 — VAT Number Assignment: Non-EU Scheme Delay Notification

**Given** a complete Non-EU scheme registration is received on date R  
**When** the system cannot assign a VAT number by the end of day 8  
**Then** the system sends an electronic notification to the taxable person by day 8, stating the expected assignment date  

**Failure condition:** No notification is sent within 8 days when assignment is delayed.

---

### AC-OSS-02-09 — VAT Number Assignment: EU Scheme — Existing DK Registration

**Given** an EU scheme applicant who is already VAT-registered in Denmark under ordinary rules  
**When** their EU scheme registration is confirmed  
**Then** no new VAT number is assigned; their existing Danish VAT number is associated with the EU scheme registration  

**Failure condition:** System assigns a new duplicate number to an already-registered applicant.

---

### AC-OSS-02-10 — VAT Number Assignment: EU Scheme — No Existing DK Registration

**Given** an EU scheme applicant who is NOT VAT-registered in Denmark under ordinary rules  
**When** the complete registration information is received on date R  
**Then** the system assigns a new unique VAT number within 8 calendar days and communicates it electronically  

**Failure condition:** Number is assigned after day 8 without a prior delay notification.

---

### AC-OSS-02-11 — EU Binding Rule: Period Enforced

**Given** a taxable person selects Denmark as identification member state under ML § 66d stk. 1 case (b) or (c)  
**When** the registration is confirmed  
**Then** the system records the binding period end date as 31 December of the calendar year 2 years after the current year  
**And** the system prevents any attempt to change the identification member state to a non-Denmark state during the binding period  

**Failure condition:** System allows identification member state change during the binding period, or binding period end date is calculated incorrectly.

---

### AC-OSS-02-12 — EU Binding Rule: Change Permitted on Loss of Eligibility

**Given** a taxable person bound to Denmark under the EU scheme binding rule  
**When** their home establishment or fixed establishment moves such that Denmark no longer qualifies as identification member state  
**Then** the system permits the identification member state change effective from the date of the change  
**And** the binding period clock resets in the new identification member state  

**Failure condition:** System blocks a legally required identification state change citing the binding rule.

---

### AC-OSS-02-13 — Change Notification: Accepted Within Deadline

**Given** a taxable person with an active Non-EU or EU registration  
**When** they submit a change notification on or before the 10th day of the month following the change  
**Then** the system updates the registration record and records the notification date  

**Failure condition:** System rejects a timely change notification.

---

### AC-OSS-02-14 — Change Notification: Late Submission Flagged

**Given** a taxable person submits a change notification after the 10th day of the month following the change  
**When** the system processes the notification  
**Then** the system records the change but flags it as a late notification  
**And** the late notification is available for compliance review  

**Failure condition:** System silently accepts a late notification with no flag, or rejects it altogether.

---

### AC-OSS-02-15 — Voluntary Deregistration: Timely Notification

**Given** a taxable person with an active Non-EU or EU registration  
**When** they submit a voluntary deregistration notification at least 15 days before the end of the preceding quarter  
**Then** the system records the deregistration with effective date = first day of the following quarter  

**Failure condition:** System rejects a timely deregistration, or sets the wrong effective date.

---

### AC-OSS-02-16 — Voluntary Deregistration: Late Notification Deferred

**Given** a taxable person submits a voluntary deregistration notification fewer than 15 days before the end of the preceding quarter  
**When** the system processes the notification  
**Then** the system defers the deregistration to the first day of the quarter one further quarter ahead  
**And** the system communicates the revised effective date to the taxable person  

**Failure condition:** System processes deregistration for the intended quarter despite missing the 15-day deadline.

---

### AC-OSS-02-17 — Voluntary Deregistration: No Re-Entry Penalty

**Given** a taxable person has voluntarily deregistered from the Non-EU or EU scheme  
**When** they attempt to re-register in any subsequent quarter  
**Then** the system accepts the new registration without penalty  
**And** no exclusion flag is set based on the prior voluntary deregistration  

**Failure condition:** System blocks re-registration citing a deregistration history.

---

### AC-OSS-02-18 — Forced Exclusion: Criterion 1 — Cessation Notification

**Given** a taxable person with an active Non-EU or EU registration notifies Skatteforvaltningen that they no longer make eligible supplies  
**When** the system processes the notification  
**Then** the system initiates forced exclusion  
**And** the exclusion effective date is the **first day of the calendar quarter** following the day the exclusion decision is sent electronically to the taxable person  

**Failure condition:** System does not initiate exclusion, or sets the wrong effective date.

---

### AC-OSS-02-19 — Forced Exclusion: Criterion 2 — Presumed Cessation

**Given** a taxable person has made no eligible supplies reported in any consumption member state for **2 consecutive calendar years**  
**When** the system detects this condition  
**Then** the system initiates forced exclusion for presumed cessation  
**And** the exclusion effective date follows the standard next-quarter rule  

**Failure condition:** System fails to detect presumed cessation after 2 years, or acts before the 2-year threshold.

---

### AC-OSS-02-20 — Forced Exclusion: Criterion 3 — Conditions No Longer Met

**Given** a taxable person no longer satisfies the eligibility conditions for the scheme they are registered in  
**When** Skatteforvaltningen determines this (based on available information including from other member states)  
**Then** the system records and executes forced exclusion  
**And** the exclusion effective date is the first day of the quarter following the decision  

**Failure condition:** System does not provide a mechanism for Skatteforvaltningen to record and execute this type of exclusion.

---

### AC-OSS-02-21 — Forced Exclusion: Criterion 4 — Persistent Non-Compliance + 2-Year Penalty

**Given** a taxable person is excluded for persistent non-compliance with scheme rules  
**When** the exclusion is recorded  
**Then** the system sets a 2-year re-registration block starting from the exclusion effective date  
**And** any re-registration attempt during the block period is rejected with a clear reason code  

**Failure condition:** System applies the 2-year block to other exclusion criteria, or fails to apply it to criterion 4.

---

### AC-OSS-02-22 — Forced Exclusion: Effective Date from Change of Establishment

**Given** a forced exclusion is triggered by a change of home establishment or fixed establishment location that ends Denmark's status as identification member state  
**When** the exclusion is recorded  
**Then** the exclusion effective date is set to the **date of the establishment change**, not the start of the following quarter  

**Failure condition:** System uses the next-quarter rule instead of the change date for this specific exclusion cause.

---

### AC-OSS-02-23 — Forced Exclusion: Only Identification Member State Authority

**Given** a taxable person is registered in the EU or Non-EU scheme with Denmark as identification member state  
**When** a forced exclusion is required  
**Then** only Skatteforvaltningen can execute the exclusion; no other entity can trigger it through the system  

**Failure condition:** System allows exclusion to be triggered by an entity other than Skatteforvaltningen.

---

### AC-OSS-02-24 — Scheme Switching: Non-EU to EU (or vice versa)

**Given** a taxable person moves their home establishment such that they lose eligibility for one scheme but gain eligibility for the other  
**When** the system processes the establishment change  
**Then** exclusion from the prior scheme is effective on the **date of the change**  
**And** registration under the new scheme may be effective on the same date (applying the early-delivery exception if applicable)  

**Failure condition:** System creates a gap period or an overlap period for the two schemes.

---

### AC-OSS-02-25 — Transitional Provision: Update Deadline Enforced

**Given** a taxable person was registered before 1 July 2021 and has not updated their identification information to meet new rules  
**When** the current date is on or after 1 April 2022  
**Then** the system flags their registration as overdue for the mandatory identification update  
**And** the system prevents new return periods from being opened until the update is completed  

**Failure condition:** System does not flag overdue pre-July-2021 registrants after 1 April 2022.

---

## 3. Measurable Success Indicators

| Indicator | Target |
|---|---|
| All Gherkin scenarios passing | 100% |
| FR coverage (FRs with ≥1 passing test) | 38/38 (FR-OSS-02-001 to FR-OSS-02-038) |
| VAT number assignment within SLA | 100% within 8 calendar days or delay notification sent |
| Effective date calculation accuracy | 100% correct for all date boundary tests |
| EU binding period end-date accuracy | 100% correct (year + 2, December 31) |
| 2-year exclusion block applied to criterion 4 only | 100% — not applied to criteria 1–3 |
| Voluntary deregistration re-entry blocked | 0% (no blocking permitted) |

---

## 4. Failure Conditions Summary

The implementation FAILS if any of the following are true:

- The EU binding rule can be bypassed by changing the identification member state mid-period without legal cause.
- Forced exclusion can be triggered by any actor other than Skatteforvaltningen.
- The 2-year re-registration block is applied to voluntary deregistration.
- A taxable person is assigned a duplicate VAT number, or an EU-scheme applicant who already has a Danish VAT number receives a second number.
- Effective dates for any registration event deviate from the rules in ML §§ 66b stk. 2–3, 66e stk. 2, 66i stk. 1, 66j stk. 1, Momsforordningen artikel 58 stk. 2.
- Non-EU/Import-scheme VAT numbers are usable for other VAT purposes.
- Pre-July-2021 registrants are not flagged after 1 April 2022.

---

## 5. Suggested Next Step

This outcome contract is ready for:
1. `petition-to-gherkin` — convert to `OSS-02.feature` (already authored alongside this contract)
2. `component-assigner` — map functional scenarios to osm2 components once component definitions exist
3. `backlog-planner` — sequence OSS-02 relative to OSS-01 (if present) and OSS-03
