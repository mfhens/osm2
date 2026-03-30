# OSS-04 — Outcome Contract: Momsangivelse og korrektioner

## Definition of Done

A feature is done when:
1. All acceptance criteria below pass in an automated test suite.
2. Every Gherkin scenario in `OSS-04.feature` has a passing step implementation.
3. No rounding of VAT amounts occurs anywhere in the filing pipeline.
4. Currency conversion uses the ECB rate for the last day of the relevant period (not the payment
   date or submission date) in all filing and correction paths.
5. The feature has been reviewed by the `code-reviewer-strict` and `scrutiny-feature-reviewer`
   agents without outstanding defects.

---

## Acceptance Criteria

### AC-1: Ordinary Return — Period and Deadline

**AC-1.1:** Given a taxable person registered under the Non-EU or EU scheme with Denmark as
identification member state, when a return is submitted for a calendar quarter (Q1–Q4), the
system accepts the return if and only if it is filed on or before the last calendar day of the
month following that quarter.

**AC-1.2:** Given a taxable person (or intermediary) registered under the Import scheme with
Denmark as identification member state, when a return is submitted for a calendar month, the
system accepts the return if and only if it is filed on or before the last calendar day of the
following month.

**AC-1.3:** Given a taxable person who made no qualifying supplies in any consumption member
state and has no corrections to include, the system accepts a zero return for the period. The
return is recorded as a valid filed return, satisfying the filing obligation for that period.

---

### AC-2: Return Content Validation

**AC-2.1 (All schemes):** The system rejects any return that does not include the taxable
person's VAT registration number. Error: missing VAT registration number.

**AC-2.2 (All schemes):** For each consumption member state declared in the return, the system
validates that the return contains: total supply value excluding VAT, at least one applicable
VAT rate, a VAT amount per rate, and a total VAT amount due. The system rejects returns where
any of these fields are absent for a declared consumption member state.

**AC-2.3 (EU scheme — dispatch from other member states):** When the return includes supplies
dispatched from a member state other than Denmark, the system validates that a VAT or tax
registration number for that dispatch member state is present in the return. The system rejects
EU-scheme returns that declare dispatch-from supplies without the required registration number.

**AC-2.4 (EU scheme — fixed establishments):** When the return includes supplies from a fixed
establishment in a member state other than Denmark, the system validates that the establishment's
VAT or tax registration number is included. The system rejects EU-scheme returns that declare
fixed-establishment supplies without the required registration number.

**AC-2.5 (Import scheme):** The system validates that Import scheme returns include the VAT
registration number(s) per momsbekendtgørelsens § 119 stk. 4. The system rejects Import returns
missing these identifiers.

---

### AC-3: Currency and Rounding

**AC-3.1:** When a return includes amounts in a currency other than DKK, the system converts
those amounts using the ECB exchange rate published on the last calendar day of the return period.
If no ECB rate is published on that day, the system uses the rate from the immediately following
ECB publication day. The converted amount in DKK is stored as the authoritative amount on the
return.

**AC-3.2:** When a return includes a correction to a prior period and the corrected amount was
originally recorded in a non-DKK currency, the system converts using the ECB rate for the last
calendar day of the period being corrected — not the ECB rate for the new filing period.

**AC-3.3:** The system does not round any VAT amount on any OSS return to the nearest whole DKK
or any other whole currency unit. A test with a computed VAT of DKK 1,234.567 must store and
transmit exactly DKK 1,234.567 (or its full-precision equivalent).

---

### AC-4: Final Return

**AC-4.1:** When a taxable person's registration is terminated (cessation, exclusion, or
identification state change), the system creates a filing obligation for a final return addressed
to the identification member state that was in effect at the time of termination. The final return
must be flagged as final in the system.

**AC-4.2:** The final return filing obligation includes all outstanding ordinary returns for prior
periods that have not yet been filed. The system prevents termination from being recorded as
complete until the final return and all outstanding prior returns have been submitted.

**AC-4.3:** When an intermediary is deregistered or changes identification member state, the
system creates final return obligations for all taxable persons the intermediary represents,
not just for the intermediary itself.

---

### AC-5: Special Period Situations

**AC-5.1 (Mid-quarter registration):** When the system records that a Non-EU or EU scheme
registration became effective from the date of first qualifying supply (not from the start of
the quarter), it generates a separate return obligation for that quarter. The separate return
covers the period from the date of first supply to the end of the quarter, not the full quarter.

**AC-5.2 (Mid-period identification state change — EU/Import):** When the identification member
state changes after the first day of a return period, the system generates two separate return
obligations: one to the old identification member state covering the period up to the change, and
one to the new identification member state covering the period from the change to the period end.
Both obligations are created for the same underlying tax period.

**AC-5.3 (Scheme switch within quarter):** When a taxable person was registered under both the
Non-EU and EU schemes during the same calendar quarter, the system generates two separate return
obligations for that quarter — one per scheme — each covering only the supplies attributable to
that scheme for the applicable sub-period.

---

### AC-6: Late Returns and Reminders

**AC-6.1:** On day 10 after a return filing deadline (counting calendar days from the day after
the deadline), if the return has not been filed, the system automatically dispatches an electronic
reminder to the taxable person or intermediary. The reminder is recorded with a timestamp.

**AC-6.2:** When the first reminder is dispatched (AC-6.1), the system records that responsibility
for subsequent reminders and for VAT collection for that period has passed to the relevant
consumption member states. The system notifies the other member states electronically that a
reminder was issued.

**AC-6.3:** The system continues to accept a late return filed by the taxable person with the
Danish identification member state portal after a reminder has been issued. Filing the late return
with the identification member state satisfies the taxable person's filing obligation even if
consumption member states have separately initiated collection proceedings.

---

### AC-7: Corrections

**AC-7.1 (Pre-July-2021 periods):** For corrections to returns for periods up to and including
Q2 2021 (Non-EU/EU) or June 2021 (Import), the system applies the pre-July-2021 correction
mechanics (separate correction return). The system must not require the taxable person to embed
the correction in a subsequent ordinary return for these periods.

**AC-7.2 (Post-June-2021 — embedded corrections accepted):** For corrections to returns for
periods from Q3 2021 (Non-EU/EU) or July 2021 (Import) onwards, the system accepts corrections
only when they are embedded in a subsequent ordinary return. The system must reject a standalone
correction submission for these periods.

**AC-7.3 (3-year window enforced):** The system rejects a correction embedded in an ordinary
return if the original filing deadline for the period being corrected is more than 3 years before
the submission date of the ordinary return containing the correction.

**AC-7.4 (Correction fields required):** When a correction is embedded in an ordinary return, the
system validates that each embedded correction specifies: the consumption member state, the
original tax period, and the corrected VAT amount. The system rejects returns where an embedded
correction is missing any of these three fields.

**AC-7.5 (Post-final-return — OSS portal rejects corrections):** After a final return has been
filed for a taxable person under a given scheme, the system rejects any attempt to submit further
corrections via the OSS portal for that taxable person and scheme combination. The system returns
an error message indicating that corrections must be submitted directly to the relevant consumption
member state.

**AC-7.6 (Beyond-3-year corrections rejected):** The system rejects, via the OSS portal, any
correction to a period whose original deadline falls more than 3 years before the submission date,
regardless of whether a final return has been filed. The error message must indicate that the
correction must be submitted directly to the relevant consumption member state under that member
state's reopening rules.

---

## Failure Conditions

The following conditions constitute a defect if they occur in production or testing:

| # | Failure condition |
|---|---|
| F-01 | A return filed after the deadline is accepted without being flagged as late. |
| F-02 | A return is filed without a VAT registration number and is not rejected. |
| F-03 | A VAT amount is rounded to the nearest whole DKK on any return. |
| F-04 | Currency conversion uses the ECB rate on the payment date or submission date rather than the last day of the relevant return period. |
| F-05 | Currency conversion for a correction in a subsequent return uses the ECB rate of the new filing period rather than the ECB rate of the period being corrected. |
| F-06 | A standalone correction return is accepted for a period from Q3 2021 / July 2021 onwards. |
| F-07 | A correction embedded in an ordinary return is accepted when the original period's deadline was more than 3 years before the submission date. |
| F-08 | The OSS portal accepts a correction for a taxable person after that person's final return has been filed. |
| F-09 | A final return is not triggered for an intermediary's represented taxable persons when the intermediary is deregistered. |
| F-10 | The system fails to issue an electronic reminder on calendar day 10 after a missed deadline. |
| F-11 | Two separate return obligations are not generated when the identification member state changes mid-period (EU or Import scheme). |
| F-12 | A single combined return is accepted instead of two separate scheme returns when a taxable person switches between Non-EU and EU schemes within the same quarter. |
