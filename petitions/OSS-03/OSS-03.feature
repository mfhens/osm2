Feature: OSS-03 — Import Scheme Registration and Deregistration
  As Skatteforvaltningen
  I need to manage the full registration lifecycle for the Import scheme (Importordningen)
  So that eligible taxable persons and intermediaries can register, make changes,
  and deregister in compliance with ML §§ 66l–66t and Momssystemdirektivet Art. 369l–369w

  Background:
    Given the osm2 system is configured with Denmark as an available identification member state for the Import scheme

  # ───────────────────────────────────────────────────────────────────────────
  # FR-01: Eligibility Validation
  # ───────────────────────────────────────────────────────────────────────────

  Scenario: Reject registration when consignment value exceeds EUR 150
    Given an applicant submits a direct Import scheme registration
    And the declared consignment value is EUR 151
    When the system processes the eligibility check
    Then the registration is rejected
    And the rejection reason is "consignment value exceeds the EUR 150 limit per ML § 66l"

  Scenario: Reject registration for excisable goods
    Given an applicant submits a direct Import scheme registration
    And the goods category is declared as "excisable goods"
    When the system processes the eligibility check
    Then the registration is rejected
    And the rejection reason is "excisable goods are excluded from the Import scheme per ML § 66l"

  Scenario: Reject direct registration for non-EU supplier without intermediary and without mutual assistance agreement
    Given an applicant submits a direct Import scheme registration
    And the applicant is established in a country without an EU mutual assistance agreement
    And no intermediary is declared
    When the system processes the eligibility check
    Then the registration is rejected
    And the rejection reason is "applicant must be EU-established or represented by an EU-established intermediary per ML § 66n stk. 1"

  Scenario: Accept registration for EU-established supplier registering directly
    Given an applicant submits a direct Import scheme registration
    And the applicant is established in an EU member state
    And the goods are imported from outside the EU
    And the consignment value is EUR 120
    And the goods are non-excisable
    When the system processes the eligibility check
    Then the registration is accepted for further processing

  Scenario: Accept registration for non-EU supplier represented by an EU-established intermediary
    Given an intermediary established in Denmark has an active Import scheme registration
    And the intermediary submits a supplier registration for a supplier established outside the EU
    And the goods are imported from outside the EU
    And the consignment value is EUR 80
    And the goods are non-excisable
    When the system processes the eligibility check
    Then the registration is accepted for further processing

  Scenario: Accept registration for supplier in a mutual-assistance-agreement country registering directly
    Given an applicant submits a direct Import scheme registration
    And the applicant is established in a country that has a mutual assistance agreement with the EU
    And the goods are imported from that country
    And the consignment value is EUR 100
    And the goods are non-excisable
    When the system processes the eligibility check
    Then the registration is accepted for further processing

  # ───────────────────────────────────────────────────────────────────────────
  # FR-02: Intermediary Registration
  # ───────────────────────────────────────────────────────────────────────────

  Scenario: Assign unique intermediary identification number within 8 days of complete application
    Given a complete intermediary registration application is submitted with all mandatory fields
    When the system processes the intermediary registration
    Then a unique intermediary identification number is assigned
    And the number is communicated to the intermediary electronically
    And the assignment occurs within 8 calendar days of receiving the complete application

  Scenario: Notify intermediary when 8-day number assignment cannot be met
    Given a complete intermediary registration application is submitted
    And the VAT number assignment cannot be completed within 8 calendar days
    When 8 calendar days elapse after receiving the application
    Then the system sends the intermediary a notification of the expected assignment date
    And the notification is sent within 8 calendar days of receiving the application

  Scenario: Intermediary identification number is scheme-exclusive and rejected in ordinary VAT filings
    Given an intermediary has been assigned Import scheme identification number "IM-DK-000123"
    When "IM-DK-000123" is submitted as the VAT registration number in an ordinary Danish VAT filing
    Then the submission is rejected
    And the rejection reason is "number is reserved for Import scheme use only per Art. 369q"

  Scenario: Reject intermediary registration without any EU establishment
    Given an intermediary registration application is submitted
    And the intermediary is established outside the EU
    And the intermediary has no fixed establishment in any EU member state
    When the system processes the eligibility check
    Then the registration is rejected
    And the rejection reason is "intermediary must be established in the EU per ML § 66m"

  Scenario: Reject intermediary registration with a missing mandatory field
    Given an intermediary registration application is submitted
    And the application is missing the "email address" field
    When the system processes the registration data
    Then the registration is rejected
    And the rejection identifies "email address" as a required field

  # ───────────────────────────────────────────────────────────────────────────
  # FR-03: Supplier Registration Under an Intermediary
  # ───────────────────────────────────────────────────────────────────────────

  Scenario: Assign unique supplier VAT number within 8 days of complete supplier data submitted by intermediary
    Given an intermediary with identification number "IM-DK-000123" is active in the Import scheme
    And the intermediary submits a complete supplier registration for supplier "ABC Trading Ltd"
    When the system processes the supplier registration
    Then a unique individual VAT registration number is assigned to "ABC Trading Ltd"
    And the number is communicated electronically to the intermediary within 8 calendar days

  Scenario: Notify intermediary when 8-day assignment for supplier cannot be met
    Given an intermediary submits a complete supplier registration
    And the VAT number assignment cannot be completed within 8 calendar days
    When 8 calendar days elapse after receiving the supplier data
    Then the system notifies the intermediary of the expected assignment date
    And the notification is sent within 8 calendar days of receiving the supplier data

  Scenario: Supplier effective date is the date of VAT number assignment — not start of next quarter
    Given an intermediary submits a complete supplier registration on "2026-04-10"
    And the VAT registration number is assigned on "2026-04-15"
    When the supplier registration is confirmed
    Then the supplier's effective registration start date is "2026-04-15"
    And the effective date is not "2026-07-01"

  Scenario: Supplier record carries non-nullable intermediary link
    Given a supplier "ABC Trading Ltd" is registered under intermediary "IM-DK-000123"
    When the system stores the supplier registration record
    Then the record contains a non-null reference to intermediary "IM-DK-000123"

  Scenario: Supplier identification member state equals the intermediary's identification member state
    Given an intermediary is established in Denmark and has Denmark as identification member state
    And the intermediary registers supplier "ABC Trading Ltd"
    When the supplier registration is confirmed
    Then the supplier's identification member state is recorded as Denmark

  Scenario: Reject supplier registration submitted by intermediary with a missing mandatory field
    Given an intermediary submits a supplier registration for a supplier established in Germany
    And the application is missing the "postal address" field
    When the system processes the registration data
    Then the registration is rejected
    And the rejection identifies "postal address" as a required field

  Scenario: Danish-established supplier under intermediary accepted without name, home country, and postal address
    Given an intermediary submits a supplier registration for a supplier established in Denmark
    And the application does not include the supplier's name, home country, or postal address
    When the system processes the registration data
    Then the registration is accepted without requiring those fields

  # ───────────────────────────────────────────────────────────────────────────
  # FR-04: Direct Supplier Registration
  # ───────────────────────────────────────────────────────────────────────────

  Scenario: Direct supplier receives unique Import-scheme VAT number within 8 days
    Given a complete direct supplier registration application is submitted
    And the applicant is established in an EU member state
    When the system processes the registration
    Then a unique individual Import scheme VAT registration number is assigned
    And the number is communicated electronically to the applicant within 8 calendar days

  Scenario: Direct supplier effective date is the date of VAT number assignment — not start of next quarter
    Given a direct supplier registration application is submitted on "2026-05-01"
    And the VAT registration number is assigned on "2026-05-06"
    When the registration is confirmed
    Then the supplier's effective registration start date is "2026-05-06"
    And the effective date is not "2026-07-01"

  Scenario: Direct registration covers all Import-scheme distance sales — no partial opt-in
    Given supplier "XYZ GmbH" is registered directly in the Import scheme
    When the supplier's registration record is confirmed
    Then the registration applies to all of "XYZ GmbH"'s distance sales of goods imported from outside the EU
    And the record contains no partial-opt-in flag permitting selective coverage

  Scenario: Danish-established direct supplier accepted without name, home country, and postal address
    Given a direct supplier registration application is submitted
    And the applicant is established in Denmark
    And the application does not include name, home country, or postal address
    When the system processes the registration data
    Then the registration is accepted without requiring those fields

  # ───────────────────────────────────────────────────────────────────────────
  # FR-05: Identification Member State — Binding Choice
  # ───────────────────────────────────────────────────────────────────────────

  Scenario: Intermediary with multiple EU establishments is bound by identification state choice for 3 years
    Given an intermediary has fixed establishments in Denmark and Germany
    And the intermediary chose Denmark as identification member state in calendar year 2026
    When the intermediary submits a request to change identification member state to Germany in calendar year 2027
    Then the system rejects the change
    And the rejection reason states the choice is binding for calendar years 2026, 2027, and 2028

  Scenario: Intermediary with single EU member state of establishment has identification state set automatically
    Given an intermediary is established solely in Denmark
    When the intermediary's registration is processed
    Then the identification member state is recorded as Denmark without requiring a manual choice

  # ───────────────────────────────────────────────────────────────────────────
  # FR-06: Joint Liability — Data Integrity
  # ───────────────────────────────────────────────────────────────────────────

  Scenario: Attempt to remove intermediary link without a valid deregistration event is rejected
    Given supplier "ABC Trading Ltd" is registered under intermediary "IM-DK-000123"
    When the system receives a request to set the intermediary link to null without a deregistration event
    Then the request is rejected
    And the rejection reason is "intermediary link may only be removed via a valid deregistration or intermediary-change event"

  # ───────────────────────────────────────────────────────────────────────────
  # FR-07: Registration Changes
  # ───────────────────────────────────────────────────────────────────────────

  Scenario: Intermediary notifies a change to supplier registration data within 10 days
    Given supplier "ABC Trading Ltd" is registered under intermediary "IM-DK-000123"
    And the supplier's email address changed on "2026-06-01"
    And the intermediary submits the change notification on "2026-06-08"
    When the system processes the change notification
    Then the supplier record is updated with the new email address
    And the change effective date "2026-06-01" and notification receipt date "2026-06-08" are both recorded

  Scenario: Intermediary change for a represented supplier updates the intermediary link
    Given supplier "ABC Trading Ltd" is registered under intermediary "IM-DK-000111"
    When the supplier notifies that intermediary "IM-DK-000222" will now represent them
    And the system processes the intermediary-change notification
    Then the supplier record references intermediary "IM-DK-000222"
    And the previous intermediary link "IM-DK-000111" is retained in history with an end date

  # ───────────────────────────────────────────────────────────────────────────
  # FR-08: Voluntary Deregistration
  # ───────────────────────────────────────────────────────────────────────────

  Scenario: Voluntary deregistration notification accepted when received at least 15 days before month end
    Given supplier "ABC Trading Ltd" is registered in the Import scheme
    And today is "2026-06-10"
    When "ABC Trading Ltd" submits a voluntary deregistration notification
    Then the notification is accepted
    And the deregistration effective date is set to "2026-07-01"

  Scenario: Voluntary deregistration notification rejected when received fewer than 15 days before month end
    Given supplier "ABC Trading Ltd" is registered in the Import scheme
    And today is "2026-06-20"
    When "ABC Trading Ltd" submits a voluntary deregistration notification intending effect from "2026-07-01"
    Then the notification is rejected
    And the rejection reason is "notification must be received at least 15 days before the end of the month preceding the intended deregistration month per ML § 66s"

  Scenario: Voluntary deregistration effective date is the first day of the next month — not next quarter
    Given supplier "ABC Trading Ltd" submits a timely voluntary deregistration notification in June 2026
    When the deregistration is processed
    Then the effective date is "2026-07-01"
    And the effective date is not "2026-10-01"

  Scenario: Intermediary submits voluntary deregistration on behalf of a represented supplier
    Given intermediary "IM-DK-000123" acts on behalf of supplier "ABC Trading Ltd"
    And today is "2026-06-05"
    When the intermediary submits a voluntary deregistration notification for "ABC Trading Ltd"
    Then the notification is accepted
    And the supplier deregistration effective date is set to "2026-07-01"

  # ───────────────────────────────────────────────────────────────────────────
  # FR-09: Forced Exclusion — Direct Supplier
  # ───────────────────────────────────────────────────────────────────────────

  Scenario: Direct supplier excluded upon notification of cessation of covered activities
    Given direct supplier "XYZ GmbH" is registered in the Import scheme
    And "XYZ GmbH" notifies that it no longer makes distance sales of goods imported from outside the EU
    When the system records the exclusion decision and sends it on "2026-06-15"
    Then "XYZ GmbH" is marked as excluded from the Import scheme
    And the exclusion effective date is "2026-07-01"

  Scenario: Direct supplier excluded for no longer meeting eligibility conditions
    Given direct supplier "XYZ GmbH" is registered in the Import scheme
    And Skatteforvaltningen determines that "XYZ GmbH" no longer meets Import scheme eligibility conditions
    When the exclusion decision is recorded
    Then "XYZ GmbH" is excluded from the Import scheme
    And the exclusion reason is recorded as "eligibility conditions no longer met per ML § 66t stk. 1 nr. 3"

  Scenario: Direct supplier presumed to have ceased after 2 years of no covered supply
    Given direct supplier "XYZ GmbH" is registered in the Import scheme
    And "XYZ GmbH" has made no covered supply in any consumption member state for 2 consecutive years
    When Skatteforvaltningen reviews the inactivity
    Then "XYZ GmbH" is excluded from the Import scheme
    And the exclusion reason is recorded as "presumed cessation of activities per Art. 58a momsforordningen"

  # ───────────────────────────────────────────────────────────────────────────
  # FR-10: Forced Exclusion — Intermediary
  # ───────────────────────────────────────────────────────────────────────────

  Scenario: Intermediary excluded for 2 consecutive quarters of inactivity
    Given intermediary "IM-DK-000123" has had no active represented suppliers using the Import scheme
    And this inactivity spans 2 consecutive filing quarters
    When Skatteforvaltningen reviews the inactivity at the end of the second consecutive quarter
    Then intermediary "IM-DK-000123" is excluded from the Import scheme identification register
    And the exclusion reason is recorded as "2 consecutive quarters of inactivity per ML § 66t stk. 2 nr. 1"

  Scenario: Intermediary excluded for repeated non-compliance
    Given intermediary "IM-DK-000123" meets the repeated non-compliance definition per FR-12
    When the exclusion decision is sent on "2026-06-15"
    Then intermediary "IM-DK-000123" is excluded from the Import scheme
    And the exclusion effective date is "2026-06-16"

  # ───────────────────────────────────────────────────────────────────────────
  # FR-11: Cascade Exclusion from Intermediary to Represented Suppliers
  # ───────────────────────────────────────────────────────────────────────────

  Scenario: Exclusion of intermediary cascades to all currently represented suppliers
    Given intermediary "IM-DK-000123" is active in the Import scheme
    And the following suppliers are registered under "IM-DK-000123":
      | supplier          |
      | ABC Trading Ltd   |
      | DEF Commerce BV   |
      | GHI Imports KG    |
    When intermediary "IM-DK-000123" is forcibly excluded from the Import scheme
    Then all of the following suppliers are simultaneously excluded from the Import scheme:
      | supplier          |
      | ABC Trading Ltd   |
      | DEF Commerce BV   |
      | GHI Imports KG    |
    And each supplier's exclusion record references "IM-DK-000123" exclusion as the triggering cause

  Scenario: Supplier under intermediary excluded when intermediary notifies end of representation
    Given supplier "ABC Trading Ltd" is registered under intermediary "IM-DK-000123"
    When intermediary "IM-DK-000123" notifies that it no longer represents "ABC Trading Ltd"
    And the exclusion decision is recorded
    Then "ABC Trading Ltd" is excluded from the Import scheme
    And the exclusion reason is recorded as "intermediary ended representation per ML § 66t stk. 3 nr. 5"

  # ───────────────────────────────────────────────────────────────────────────
  # FR-13: Exclusion Effective Date
  # ───────────────────────────────────────────────────────────────────────────

  Scenario: Standard exclusion effective date is first day of next calendar month — not next quarter
    Given an exclusion decision for supplier "XYZ GmbH" is sent on "2026-06-15"
    And the exclusion ground is "cessation of activities"
    When the exclusion effective date is computed
    Then the effective date is "2026-07-01"
    And the effective date is not "2026-10-01"

  Scenario: Exclusion due to change of establishment takes effect from the date of the change
    Given an exclusion is triggered for supplier "XYZ GmbH"
    And the exclusion ground is "change of place of business"
    And the establishment change occurred on "2026-06-10"
    When the exclusion effective date is computed
    Then the effective date is "2026-06-10"

  Scenario: Exclusion due to repeated non-compliance takes effect the day after decision is sent
    Given an exclusion decision for supplier "XYZ GmbH" is sent on "2026-06-15"
    And the exclusion ground is "repeated non-compliance"
    When the exclusion effective date is computed
    Then the effective date is "2026-06-16"

  Scenario: Import-scheme VAT number valid for up to 2 months after standard exclusion
    Given supplier "XYZ GmbH" is excluded with effective date "2026-07-01"
    And the exclusion ground is "cessation of activities"
    When the validity of VAT number "IOSS-DK-XYZ-001" is checked on "2026-08-15"
    Then the VAT number is valid for import customs purposes
    And the validity expires no later than "2026-09-01"

  Scenario: Import-scheme VAT number immediately invalid after repeated non-compliance exclusion
    Given supplier "XYZ GmbH" is excluded for "repeated non-compliance" with effective date "2026-06-16"
    When the validity of VAT number "IOSS-DK-XYZ-001" is checked on "2026-06-16"
    Then the VAT number is invalid
    And no grace period applies

  # ───────────────────────────────────────────────────────────────────────────
  # FR-14: 2-Year Re-registration Bar
  # ───────────────────────────────────────────────────────────────────────────

  Scenario: Taxable person barred from re-registration for 2 years after exclusion for repeated non-compliance
    Given supplier "XYZ GmbH" was excluded for repeated non-compliance in filing period "2026-M06"
    When "XYZ GmbH" attempts to submit a new Import scheme registration in "2028-M05"
    Then the registration is rejected
    And the rejection reason states re-registration is not permitted until filing period "2028-M07"

  Scenario: Supplier excluded solely due to intermediary non-compliance is not subject to the 2-year bar
    Given supplier "ABC Trading Ltd" was excluded because intermediary "IM-DK-000123" was excluded for repeated non-compliance
    And "ABC Trading Ltd" had no personal non-compliance record
    And the supplier's exclusion record states the cause as "intermediary non-compliance only"
    When "ABC Trading Ltd" attempts to submit a new Import scheme registration
    Then the registration application is accepted for eligibility review
    And the 2-year bar is not applied to "ABC Trading Ltd"

  # ───────────────────────────────────────────────────────────────────────────
  # AC-13: Import Scheme VAT Number Isolation
  # ───────────────────────────────────────────────────────────────────────────

  Scenario: Import scheme VAT number is rejected in an ordinary Danish VAT filing flow
    Given supplier "XYZ GmbH" holds Import scheme VAT number "IOSS-DK-XYZ-001"
    When "IOSS-DK-XYZ-001" is submitted as the VAT registration number in an ordinary Danish VAT filing
    Then the submission is rejected
    And the rejection reason is "this number is reserved for Import scheme use only and cannot be used in ordinary VAT registration per Art. 369q"
