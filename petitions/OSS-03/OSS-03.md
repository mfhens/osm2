# OSS-03 — Registrering og afmeldelse: Importordningen

**Status:** Approved  
**Dato:** 2026-03-30  
**Forfatter:** prompt-to-requirements  
**Version:** 1.0

---

## Sammenfatning

Denne petition dækker den fulde registreringslivscyklus for Importordningen (IOSS — Import One Stop Shop) i osm2-systemet. Importordningen er arkitektonisk og juridisk distinkt fra Ikke-EU-ordningen og EU-ordningen: den indfører **formidlerrollen** (intermediæren), opererer på **månedlig** (ikke kvartalsvis) kadence, og sætter registreringens virkningstidspunkt til **datoen for tildeling af momsnummeret** — ikke til kvartalets første dag.

---

## Kontekst og motivation

Importordningen transponerer Rådsdirektiv 2017/2455 og 2019/1995 (indsat som ML §§ 66l–66t og Momssystemdirektivet artiklerne 369l–369w). Den dækker fjernsalg af varer indført fra tredjelande i forsendelser med reel værdi ≤ EUR 150 (punktafgiftspligtige varer undtaget).

Den afgørende arkitektoniske forskel fra OSS-02 er **formidleren**: en EU-etableret formidler kan tilmelde sig ordningen på vegne af én eller flere afgiftspligtige sælgere og hæfter solidarisk med disse sælgere for momsbetalingen (ML § 66m; Art. 369l). Formidleren udgør et overordnet identitetsniveau i datamodellen, som sælgeren er knyttet til.

osm2 skal understøtte:

1. Direkte tilmelding af EU-etablerede sælgere (uden formidler)
2. Formidlerregistrering som selvstændig entitet og administration af repræsenterede sælgere
3. Frivillig afmeldelse på månedlig kadence
4. Tvangsafmeldelse/udelukkelse, herunder kaskadering fra formidler til sælger

---

## Functional Requirements

### FR-01: Eligibility Validation

The system must validate that a registration applicant for the Import scheme satisfies at least one of the following eligibility conditions before accepting their registration (ML § 66n stk. 1; Art. 369m stk. 1):

a) The applicant is an EU-established taxable person making distance sales of goods imported from outside the EU.  
b) The applicant (regardless of establishment) is represented by an EU-established intermediary and makes distance sales of goods imported from outside the EU.  
c) The applicant is established in a non-EU country with which the EU has concluded a mutual assistance agreement, and makes distance sales from that country.

The system must additionally validate that the goods covered are (ML § 66l; Art. 369l stk. 1):

- Imported from outside the EU
- In consignments with an intrinsic value not exceeding EUR 150
- Not excisable goods

### FR-02: Intermediary Registration

The system must support registration of an intermediary as a distinct principal entity with its own identity in the Import scheme. The following must apply:

a) An intermediary is registered independently from any individual supplier.  
b) Upon successful registration, the intermediary is assigned a unique intermediary identification number within 8 calendar days of receiving complete registration data (Momsbekendtgørelsens § 119 stk. 2; Art. 369q).  
c) If assignment cannot be completed within 8 calendar days, the system must notify the intermediary of the expected assignment date within the 8-day window.  
d) The intermediary identification number is exclusively usable within the Import scheme. It cannot be used for ordinary Danish VAT purposes (Art. 369q).  
e) An intermediary can represent multiple suppliers simultaneously. Each represented supplier is a distinct sub-registration linked to the intermediary's identification member state.

The following data must be captured at intermediary registration (Momsbekendtgørelsens § 119 stk. 1):

1. Trading names (if different from legal name)
2. Email address
3. Contact person for the scheme
4. Telephone number
5. Bank details
6. VAT/tax registration numbers for fixed establishments in other EU member states, including trading names and postal addresses for those establishments
7. Previous and current registrations as intermediary

### FR-03: Supplier Registration Under an Intermediary

When an intermediary registers a supplier, the system must:

a) Record the supplier as a sub-registration linked to the intermediary account.  
b) Assign a unique individual VAT registration number to the supplier within 8 calendar days of receiving complete supplier data (Momsbekendtgørelsens § 119 stk. 2 or stk. 5; Art. 369q).  
c) If assignment cannot be completed within 8 calendar days, notify the intermediary (acting on behalf of the supplier) of the expected date within the 8-day window.  
d) Set the effective start date of the supplier's registration to the date the VAT registration number is assigned — not to the start of the next calendar quarter (ML § 66n stk. 3; Art. 57d momsforordningen).  
e) Record the identification member state for the supplier as the intermediary's identification member state, not independently determined by the supplier.  
f) The VAT registration number assigned to the supplier is exclusively usable within the Import scheme and cannot be used for other VAT purposes (Art. 369q).

The following data must be captured for each supplier under an intermediary (Momsbekendtgørelsens § 119 stk. 3):

1. Name, including trading names
2. Home country and VAT or tax registration number in the home country
3. Postal address
4. Email address
5. Contact person for the scheme
6. Telephone number
7. Websites
8. VAT/tax registration numbers for fixed establishments in other EU member states, including trading names and postal addresses
9. Previous and current OSS registrations (Import scheme, EU scheme, Non-EU scheme)

**Exception:** If the supplier is established in Denmark, the intermediary need not supply the supplier's name, home country, or postal address. If the home country is a third country but the supplier is established in Denmark via a fixed establishment, the Danish VAT or tax number must be provided additionally (Momsbekendtgørelsens § 119 stk. 4).

### FR-04: Direct Supplier Registration (Without Intermediary)

The system must support registration of EU-established suppliers who register directly without using an intermediary. For direct registrants:

a) The applicant submits registration data electronically to Skatteforvaltningen (ML § 66n stk. 2).  
b) A unique individual VAT registration number is assigned within 8 calendar days of receiving complete registration data (Momsbekendtgørelsens § 118 stk. 4; Art. 369q).  
c) If assignment cannot be completed within 8 calendar days, the system must notify the applicant of the expected date within the 8-day window.  
d) The effective start date is the date the VAT registration number is assigned (ML § 66n stk. 3).  
e) Registration covers ALL the taxable person's distance sales of goods imported from outside the EU — they cannot selectively apply it to a subset of their sales (ML § 66n stk. 4; Art. 369m stk. 1).

The following data must be captured (Momsbekendtgørelsens § 118 stk. 1):

1. Name, including trading names
2. Home country and VAT or tax registration number in the home country
3. Postal address
4. Email address
5. Contact person for the scheme
6. Telephone number
7. Bank details
8. Websites
9. VAT/tax registration numbers for fixed establishments in other EU member states, including trading names and postal addresses for those establishments
10. Previous and current OSS registrations (Import scheme, EU scheme, Non-EU scheme)

**Exception:** If the applicant's place of business is in Denmark, the system need not require name, home country, and postal address. If the home country is a third country but the person is established in Denmark via a fixed establishment, the Danish VAT or tax number must be provided additionally (Momsbekendtgørelsens § 118 stk. 2).

### FR-05: Identification Member State Determination

The system must determine and record the identification member state according to the following rules (ML § 66m; Art. 369l):

**For a direct registrant (no intermediary):**

- If established in a member state: that member state is the identification state.
- If established outside the EU but with one or more fixed establishments in the EU: the member state the applicant nominates from among those where it has a fixed establishment. This choice is binding for the calendar year of nomination and the 2 subsequent calendar years.
- If established in a non-EU mutual-assistance-agreement country: the member state the applicant chooses.

**For an intermediary-represented supplier:**

- If the intermediary is established in a member state: that member state.
- If the intermediary is established outside the EU but with one or more fixed establishments in the EU: the member state the intermediary nominates. Binding for the calendar year of nomination and the 2 subsequent calendar years.

Denmark may be chosen as identification member state by intermediaries not established in Denmark (ML § 66m).

### FR-06: Joint Liability — Data Model Requirement

The system must record and maintain the joint liability relationship between an intermediary and each supplier the intermediary represents (ML § 66m; Art. 369l solidarisk hæftelse):

a) Each supplier registration under an intermediary must carry an explicit reference to the intermediary's identification record.  
b) The system must flag that both the intermediary and the represented supplier are liable for VAT obligations arising from the supplier's use of the scheme.  
c) The data model must not permit a supplier to exist in the Import scheme under an intermediary without a valid, active intermediary link.  
d) An attempt to remove the intermediary link without a valid deregistration or intermediary-change event must be rejected.

### FR-07: Registration Changes — 10-Day Notification Obligation

The system must accept and process notifications of changes to registration data (DA16.3.5.5; Momsbekendtgørelsens § 120 / § 121):

a) The taxable person (or their intermediary) must notify Skatteforvaltningen of any change to registration data within 10 days of the change occurring.  
b) The system must record the date the change notification was received and the effective date of the change.  
c) If an intermediary changes for a represented supplier, the system must update the supplier's intermediary link and re-evaluate the identification member state if required.

### FR-08: Voluntary Deregistration (ML § 66s; Art. 57g stk. 2)

The system must allow a taxable person (directly registered) or an intermediary (on behalf of a supplier, or for themselves) to submit a voluntary deregistration notification:

a) The notification must be received by Skatteforvaltningen at least 15 days before the end of the calendar month preceding the intended month of deregistration.  
b) Deregistration takes effect from the first day of the calendar month following the notification deadline. This is a monthly cadence — not the quarterly cadence used in the Non-EU/EU schemes.  
c) The deregistered entity loses authorisation to use the scheme for supplies made from the effective date.  
d) The system must validate the 15-day-before-month-end timing rule and reject notifications that arrive too late to take effect in the intended month.

### FR-09: Forced Exclusion — Direct Supplier (ML § 66t stk. 1)

The system must support forced exclusion of a directly registered supplier on any of the following grounds:

a) The taxable person notifies that they no longer make distance sales of goods imported from outside the EU.  
b) It can otherwise be assumed that the taxable person's covered activities have ceased. A taxable person is presumed to have ceased if they have made no covered supplies in any consumption member state for 2 consecutive years (Art. 58a momsforordningen).  
c) The taxable person no longer meets the eligibility conditions for the scheme.  
d) The taxable person repeatedly fails to comply with the rules for the scheme (criteria defined in FR-12).

### FR-10: Forced Exclusion — Intermediary (ML § 66t stk. 2)

The system must support forced exclusion of an intermediary on any of the following grounds:

a) The intermediary has not acted as intermediary for any taxable person using the Import scheme for 2 consecutive quarters.  
b) The intermediary no longer meets the conditions required to act as intermediary.  
c) The intermediary repeatedly fails to comply with the rules for the scheme (criteria defined in FR-12).

### FR-11: Forced Exclusion — Supplier Under Intermediary (ML § 66t stk. 3)

The system must support forced exclusion of an intermediary-represented supplier on any of the following grounds:

a) The intermediary notifies that the supplier's distance sales of imported goods have ceased.  
b) It can otherwise be assumed that the supplier's covered activities have ceased (2-year presumption per Art. 58a).  
c) The supplier no longer meets the eligibility conditions.  
d) The supplier repeatedly fails to comply with the scheme rules (criteria defined in FR-12).  
e) The intermediary notifies that they no longer represent the supplier.

**Cascade rule:** When an intermediary is forcibly excluded under FR-10, the system must automatically exclude ALL suppliers currently represented by that intermediary (Art. 58 stk. 5 momsforordningen). Each supplier's exclusion record must reference the intermediary exclusion as the triggering cause.

### FR-12: Repeated Non-Compliance — Operative Definition

The system must implement the regulatory definition of "repeated non-compliance" as the basis for exclusion on that ground (Art. 58b stk. 2 momsforordningen). A taxable person or intermediary is deemed to repeatedly fail to comply in at least the following cases:

a) The identification member state has sent reminders under Art. 60a for the three immediately preceding filing periods, and the VAT return was not submitted within 10 days of each reminder being sent.  
b) The identification member state has sent payment reminders under Art. 63a for the three immediately preceding filing periods, and the full declared VAT amount was not paid within 10 days of each reminder (unless the outstanding amount is less than EUR 100 per period).  
c) After a request from the identification member state and one month after a subsequent reminder, the taxable person or intermediary has failed to make records available as required by Art. 369, 369k, or 369x of the VAT Directive.

### FR-13: Exclusion Effective Date — Import Scheme (Art. 58 stk. 3–4 momsforordningen)

The system must compute and record the correct effective date of exclusion for the Import scheme:

a) **General rule:** effective from the first day of the calendar month after the exclusion decision is sent electronically to the taxable person or intermediary.  
b) **Exception — change of establishment:** if exclusion arises from a change in the place of business or fixed establishment, effective from the date the change occurred.  
c) **Exception — repeated non-compliance:** effective from the day after the exclusion decision is sent electronically.  
d) After exclusion under rules (a) or (b), the VAT registration number remains valid for import customs purposes for a maximum of 2 months from the exclusion effective date, to allow completion of imports for goods already sold.  
e) After exclusion under rule (c) (repeated non-compliance), no 2-month grace period applies; the VAT number is immediately invalid.

### FR-14: Post-Exclusion Bar from Re-Registration (ML § 66t stk. 4; Art. 58b stk. 1)

The system must enforce a 2-year bar on re-registration for taxable persons or intermediaries excluded for repeated non-compliance:

a) The 2-year bar period is calculated from the filing period in which the exclusion occurred.  
b) The 2-year bar does **not** apply to a supplier whose exclusion was caused solely by their intermediary's non-compliance — not by the supplier's own non-compliance. The system must record the causal basis of each exclusion (supplier's own vs. intermediary-caused) to enforce this exception correctly (ML § 66t stk. 4, 2. pkt.).

---

## Non-Functional Requirements

No non-functional requirements have been stated for this petition. Any NFRs must be introduced via a separate petition.

---

## Constraints and Assumptions

- **Constraint:** The VAT registration number assigned for the Import scheme must be stored separately from any ordinary Danish VAT registration number (momsregistreringsnummer) held by the same entity. The two cannot be used interchangeably (Art. 369q).
- **Constraint:** An intermediary-represented supplier's effective registration date is always the date of VAT number assignment, not an applicant-chosen date.
- **Constraint:** The identification member state choice (where the intermediary or direct registrant has multiple EU establishments) is binding for the current calendar year and the 2 following calendar years. The system must prevent changes to this choice within the binding window.
- **Assumption:** This petition covers only the Denmark-as-identification-member-state path. Interactions with other member states' registration systems are out of scope.
- **Assumption:** The electronic channel for all notifications and decisions is provided by the system's existing infrastructure. This petition does not define delivery mechanisms.
- **Assumption:** "Repeated non-compliance" tracking (reminders sent, dates, counts) draws on data from OSS-04 (filing) and OSS-05 (payment) subsystems. Those subsystems are out of scope for this petition but must provide the trigger signals.

---

## Out of Scope

The following are explicitly outside the scope of this petition:

- Non-EU scheme registration and deregistration (OSS-02)
- EU scheme registration and deregistration (OSS-02)
- VAT return filing under the Import scheme (OSS-04)
- Payment processing under the Import scheme (OSS-05)
- Record-keeping obligations under Art. 369x
- Cross-member-state data exchange protocols (VIES / OSS portal)
- Platform / electronic marketplace rules (ML § 4c stk. 2)
- VAT refund and deduction rules under ML § 66q (OSS-04 scope)

---

## Key References

| Reference | Topic |
|---|---|
| ML § 66l | Scope of Import scheme — covered supplies |
| ML § 66m | Identification member state; joint liability (solidarisk hæftelse) |
| ML § 66n | Eligibility; registration process; effective date; scheme-wide coverage |
| ML § 66o | Time of supply (payment acceptance) — informational context |
| ML § 66s | Voluntary deregistration — monthly cadence |
| ML § 66t | Forced exclusion — direct supplier, intermediary, and cascade |
| Momssystemdirektivet Art. 369l–369w | EU primary legislation basis for Import scheme |
| Momsforordningen Art. 57d | Effective date rule (date of number assignment) |
| Momsforordningen Art. 58, 58a, 58b, 58c | Exclusion procedure, presumption of cessation, repeated non-compliance definition, post-exclusion obligations |
| Momsforordningen Art. 60a, 63a | Reminder obligations triggering repeated non-compliance |
| Momsforordningen Art. 369q | VAT number assignment for Import scheme |
| Momsbekendtgørelsens § 118 | Direct supplier registration data |
| Momsbekendtgørelsens § 119 | Intermediary and represented supplier registration data |
| DA16.3.4 | Importordningen — general overview |
| DA16.3.5.1 | Registration and effective date |
| DA16.3.5.2 | Registration data fields |
| DA16.3.5.3 | VAT number assignment (8-day rule) |
| DA16.3.5.5 | Changes (10-day notification) |
| DA16.3.5.6 | Forced exclusion and deregistration |
| DA16.3.5.7 | Voluntary deregistration |
