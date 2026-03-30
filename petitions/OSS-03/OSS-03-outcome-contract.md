# OSS-03 — Outcome Contract

**Petition:** OSS-03 — Registrering og afmeldelse: Importordningen  
**Version:** 1.0  
**Dato:** 2026-03-30

---

## Definition of Done

A functional requirement from OSS-03 is considered done when:

1. All Gherkin scenarios in `OSS-03.feature` that correspond to that requirement pass in the test suite.
2. No regression is introduced in OSS-02 (Non-EU / EU scheme) registration behaviour.
3. All data stored is compliant with FR-06 (joint liability linkage is explicit, non-nullable, and referentially intact).
4. The effective-date logic is verified by automated tests to produce monthly (not quarterly) cadence for both voluntary deregistration and forced exclusion.

---

## Acceptance Criteria

### AC-01: Eligibility Gate (FR-01)

- A registration application that specifies a consignment value exceeding EUR 150 is rejected with a specific eligibility error identifying the value limit.
- A registration application for excisable goods is rejected with a specific eligibility error identifying the goods category restriction.
- A registration application for a supplier not established in the EU and not represented by an EU-established intermediary and not from a mutual-assistance-agreement country is rejected with a specific eligibility error.
- A valid application satisfying at least one of the three eligibility paths (FR-01a, FR-01b, FR-01c) and both goods conditions (non-excisable, ≤ EUR 150) is accepted for further processing.

### AC-02: Intermediary Registration (FR-02)

- A successfully submitted, complete intermediary registration results in a unique intermediary identification number being assigned and communicated electronically within 8 calendar days of receiving the complete application.
- If the 8-day deadline cannot be met, a notification of the expected assignment date is sent within 8 calendar days of receiving the application.
- The assigned intermediary identification number is not found in the ordinary VAT register; it is scheme-specific and cannot be used for ordinary VAT filings.
- An intermediary registration application without any EU establishment (no member state of establishment, no fixed establishment in any EU member state) is rejected.
- An intermediary registration application with any mandatory field missing (per FR-02 data list) is rejected with a field-level error identifying the missing field.

### AC-03: Supplier Registration Under Intermediary (FR-03)

- A supplier registered by an intermediary is assigned a unique individual VAT registration number within 8 calendar days of the intermediary submitting complete supplier data.
- If the 8-day deadline cannot be met, the intermediary is notified of the expected assignment date within 8 calendar days.
- The supplier's effective registration start date equals the date of VAT number assignment — not the start of the next calendar quarter.
- The supplier record carries an explicit, non-nullable reference to the intermediary's identification record at all times.
- The identification member state for the supplier equals the intermediary's identification member state.
- A supplier registration application with any mandatory field missing (per FR-03 data list, subject to the Danish-establishment exception) is rejected with a field-level error.
- A supplier established in Denmark can be registered by an intermediary without providing the supplier's name, home country, or postal address, and the application is accepted.

### AC-04: Direct Supplier Registration (FR-04)

- A directly registered supplier receives a unique Import-scheme VAT registration number within 8 calendar days of submitting a complete application.
- The supplier is notified of the expected date if assignment cannot be completed within 8 calendar days.
- The effective start date equals the date of VAT number assignment, not the start of the next calendar quarter.
- The registration record indicates that the scheme applies to all of the taxable person's distance sales of imported goods (no partial-opt-in flag is permitted).
- A supplier established in Denmark is not required to provide name, home country, or postal address, and the application is accepted without those fields.

### AC-05: Identification Member State — Binding Choice (FR-05)

- An intermediary or direct registrant with multiple EU fixed establishments who has declared an identification member state cannot change that choice within the current calendar year and the 2 subsequent calendar years. An attempt to change is rejected with an informative error stating the binding period.
- An intermediary or direct registrant with a single EU member state of establishment has that state recorded automatically without a choice being required.

### AC-06: Joint Liability — Data Integrity (FR-06)

- Every supplier record in the Import scheme that was registered via an intermediary carries a non-null, referentially intact intermediary link at all times.
- An attempt to remove or null the intermediary link on a supplier record without a valid deregistration or intermediary-change event is rejected by the system.

### AC-07: Registration Changes (FR-07)

- A change notification received within 10 days of the effective date of the change is accepted and both dates (change effective date and notification receipt date) are recorded.
- A change of intermediary for a supplier updates the supplier's intermediary link and records the end date of the previous link in history.

### AC-08: Voluntary Deregistration — Timing (FR-08)

- A voluntary deregistration notification received at least 15 days before the end of the current calendar month is accepted and the effective deregistration date is set to the first day of the next calendar month.
- A voluntary deregistration notification received fewer than 15 days before the end of the current calendar month is rejected, with an informative message citing the 15-day rule.
- The effective deregistration date is the first day of the next calendar **month** — not the first day of the next calendar quarter.
- The deregistered entity's VAT number is marked inactive from the effective deregistration date; no supplies may be reported under the scheme from that date.
- An intermediary may submit a voluntary deregistration on behalf of a represented supplier, and the timing rules apply identically.

### AC-09: Forced Exclusion — Direct Supplier (FR-09)

- A direct supplier is excluded when any of the four exclusion grounds (FR-09a–d) is recorded by Skatteforvaltningen.
- The exclusion record states the ground for exclusion.
- The effective date conforms to FR-13 (see AC-11).

### AC-10: Forced Exclusion — Intermediary and Cascade (FR-10, FR-11)

- An intermediary is excluded when it has had no active represented suppliers making use of the Import scheme for 2 consecutive calendar quarters.
- An intermediary is excluded when it repeatedly fails to comply with scheme rules, per the definition in FR-12.
- When an intermediary is forcibly excluded, all suppliers currently represented by that intermediary are simultaneously excluded. No active supplier under the excluded intermediary remains in the scheme.
- Each cascade-excluded supplier record references the intermediary exclusion event as the triggering cause.
- A supplier under an intermediary is individually excluded (without cascading from intermediary exclusion) when grounds FR-11a–e are triggered directly for that supplier.

### AC-11: Exclusion Effective Date (FR-13)

- **Standard exclusion:** effective date is the first day of the calendar month following the date the exclusion decision is sent electronically. The effective date is not the first day of the next calendar quarter.
- **Change-of-establishment exclusion:** effective date is the date the establishment change occurred.
- **Repeated non-compliance exclusion:** effective date is the calendar day after the exclusion decision is sent electronically.
- After a standard or change-of-establishment exclusion, the Import-scheme VAT number remains valid for import customs purposes for a maximum of 2 calendar months from the effective date.
- After a repeated non-compliance exclusion, the VAT number is invalid with immediate effect from the effective date; no 2-month grace period is granted.

### AC-12: 2-Year Re-registration Bar (FR-14)

- A taxable person or intermediary excluded for repeated non-compliance cannot submit a new Import scheme registration during the 2-year bar period calculated from the filing period of exclusion. The system rejects such an application with an informative message citing the bar expiry date.
- A supplier whose exclusion was caused solely by their intermediary's non-compliance (not by the supplier's own conduct) is not subject to the 2-year bar. The system permits a new registration application from such a supplier immediately after exclusion.
- The causal basis of each exclusion (supplier's own non-compliance vs. intermediary-caused) is stored on the exclusion record and is readable for the purpose of evaluating re-registration eligibility.

### AC-13: Import Scheme VAT Number Isolation (FR-02d, FR-03f, FR-04b)

- The VAT registration number assigned under the Import scheme is stored in a distinct register from ordinary Danish VAT registration numbers.
- Submitting an Import-scheme VAT number in an ordinary Danish VAT filing is rejected by the system with an informative error identifying the number as scheme-exclusive.

---

## Failure Conditions

The following constitute failures that block acceptance:

- A supplier exists in the Import scheme under an intermediary without a non-null intermediary link.
- A supplier's effective start date is set to the start of the next calendar quarter rather than the date of VAT number assignment.
- A voluntary deregistration takes effect on the first day of the next quarter rather than the first day of the next month.
- A forced exclusion effective date defaults to the first day of the next quarter rather than the first day of the next month (standard rule).
- An intermediary exclusion does not cascade to all currently represented suppliers.
- The 2-year re-registration bar is applied to a supplier whose exclusion was caused solely by intermediary non-compliance.
- The 2-year re-registration bar is not applied to an intermediary or taxable person excluded for their own repeated non-compliance.
- A VAT number assigned under the Import scheme is accepted as a valid VAT identifier in ordinary Danish VAT registration or filing flows.
- After a repeated non-compliance exclusion, a 2-month grace period is granted on the VAT number (it must not be).
- After a standard exclusion, no grace period is granted on the VAT number (it must be — up to 2 months).
