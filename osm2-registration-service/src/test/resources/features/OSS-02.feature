# OSS-02 — Registrering og afmeldelse: Ikke-EU-ordning og EU-ordning
# Petition: OSS-02
# Legal basis: ML §§ 66a–66j; Momsbekendtgørelsen §§ 115–119; Momsforordningen artikler 57d–58c
# References: docs/references/DA16.3.5.1 through DA16.3.5.7

Feature: Non-EU scheme registration lifecycle
  As a taxable person not established in any EU member state
  I want to register electronically with Denmark as identification member state under the Non-EU scheme
  So that I can discharge my EU VAT obligations through a single identification member state

  Background:
    Given Denmark is the identification member state for all registrations in this feature
    And the system is the authoritative registration database for Skatteforvaltningen

  # ── FR-OSS-02-001, FR-OSS-02-002 ──────────────────────────────────────────

  Scenario: Non-EU scheme registration accepted for eligible applicant
    Given a taxable person named "Acme Services Ltd" based in a non-EU country
    And they declare no fixed establishment in any EU member state
    When they submit a complete electronic Non-EU scheme registration notification on 2024-02-15
    Then the registration is accepted with status "PENDING_VAT_NUMBER"
    And the desired start date "2024-02-15" is stored against the registration

  Scenario: Non-EU scheme registration rejected when applicant has EU fixed establishment
    Given a taxable person who has a fixed establishment in Germany
    When they attempt to register under the Non-EU scheme
    Then the registration is rejected with reason "APPLICANT_HAS_EU_ESTABLISHMENT"
    And no registration record is created

  # ── FR-OSS-02-003 ──────────────────────────────────────────────────────────

  Scenario: Normal effective date is first day of quarter following notification
    Given a taxable person submits a Non-EU scheme registration notification on 2024-02-15
    And no eligible delivery has been made before the notification date
    When the system calculates the effective date
    Then the registration effective date is 2024-04-01

  Scenario: Notification on first day of quarter produces next quarter as effective date
    Given a taxable person submits a Non-EU scheme registration notification on 2024-04-01
    And no eligible delivery has been made before the notification date
    When the system calculates the effective date
    Then the registration effective date is 2024-07-01

  # ── FR-OSS-02-004 ──────────────────────────────────────────────────────────

  Scenario: Effective date is first delivery date when notified within 10-day window
    Given a taxable person makes their first eligible Non-EU scheme delivery on 2024-02-08
    When they submit the registration notification on 2024-03-10
    Then the registration effective date is 2024-02-08
    And the early-delivery exception is applied

  Scenario: Notification submitted exactly on day 10 of following month qualifies for early-delivery exception
    Given a taxable person makes their first eligible Non-EU scheme delivery on 2024-01-20
    When they submit the registration notification on 2024-02-10
    Then the registration effective date is 2024-01-20

  # ── FR-OSS-02-005 ──────────────────────────────────────────────────────────

  Scenario: Late notification after early delivery forfeits exception and reverts to quarter rule
    Given a taxable person makes their first eligible Non-EU scheme delivery on 2024-01-20
    When they submit the registration notification on 2024-02-11
    Then the early-delivery exception is NOT applied
    And the registration effective date is 2024-04-01

  # ── FR-OSS-02-009 ──────────────────────────────────────────────────────────

  Scenario: Non-EU registration rejected when mandatory identification field is missing
    Given a taxable person attempts to register under the Non-EU scheme
    When the registration submission is missing the "bank_details" field
    Then the system rejects the submission with validation error "MISSING_REQUIRED_FIELD: bank_details"

  Scenario: Non-EU registration accepted with all 9 mandatory identification fields present
    Given a taxable person submits a Non-EU scheme registration with all mandatory fields:
      | field                    | value                      |
      | name                     | Acme Services Ltd          |
      | trading_names            | Acme Digital               |
      | home_country             | US                         |
      | home_country_tax_number  | EIN-12-3456789             |
      | postal_address           | 123 Main St, New York, USA |
      | email                    | vat@acme.com               |
      | contact_person           | Jane Doe                   |
      | phone                    | +1-555-0100                |
      | bank_details             | IBAN US12 3456 7890 1234   |
      | websites                 | https://acme.com           |
      | prior_registrations      | none                       |
    When the submission is processed
    Then the registration is accepted with status "PENDING_VAT_NUMBER"

  # ── FR-OSS-02-013 through FR-OSS-02-015 ────────────────────────────────────

  Scenario: Non-EU scheme VAT number assigned within 8 days
    Given a complete Non-EU scheme registration is received on 2024-03-01
    When 7 calendar days have elapsed since receipt
    Then the system has assigned a unique VAT registration number
    And the number is communicated electronically to the taxable person
    And the number is flagged as "NON_EU_SCHEME_ONLY"
    And the registration status is "ACTIVE"

  Scenario: Delay notification sent when Non-EU VAT number cannot be assigned within 8 days
    Given a complete Non-EU scheme registration is received on 2024-03-01
    And the system cannot complete the number assignment by 2024-03-09
    When 2024-03-09 is reached (day 8 boundary)
    Then the system sends an electronic delay notification to the taxable person
    And the notification states the expected assignment date

  Scenario: Non-EU VAT number is rejected for use in ordinary VAT context
    Given a taxable person holds a Non-EU scheme VAT number "EU372012345"
    When they attempt to use "EU372012345" for an ordinary Danish VAT return
    Then the system rejects the use with reason "NUMBER_RESTRICTED_TO_NON_EU_SCHEME"


Feature: EU scheme registration lifecycle
  As a taxable person eligible for the EU One-Stop-Shop scheme
  I want to register electronically with Denmark as identification member state under the EU scheme
  So that I can declare and pay EU VAT on eligible intra-EU supplies through a single registration

  # ── FR-OSS-02-006 through FR-OSS-02-008 ────────────────────────────────────

  Scenario: EU scheme registration accepted for intra-EU distance sales operator
    Given a taxable person established in Denmark makes intra-EU distance sales of goods
    When they submit a complete EU scheme registration notification on 2024-05-10
    Then the registration is accepted with status "PENDING_VAT_NUMBER"
    And the effective date is 2024-07-01

  Scenario: EU scheme early-delivery exception applies with timely notification
    Given a taxable person makes their first eligible EU scheme delivery on 2024-05-05
    When they submit the registration notification on 2024-06-10
    Then the registration effective date is 2024-05-05

  Scenario: EU scheme early-delivery exception forfeited by late notification
    Given a taxable person makes their first eligible EU scheme delivery on 2024-05-05
    When they submit the registration notification on 2024-06-11
    Then the early-delivery exception is NOT applied
    And the registration effective date is 2024-07-01

  # ── FR-OSS-02-010 through FR-OSS-02-012 ────────────────────────────────────

  Scenario: EU registration rejected when mandatory field from §117 is missing
    Given a taxable person attempts to register under the EU scheme
    When the registration submission is missing the "email" field
    Then the system rejects the submission with validation error "MISSING_REQUIRED_FIELD: email"

  Scenario: EU scheme registration requires electronic-interface declaration
    Given a taxable person is an electronic interface under ML § 4c stk. 2
    When they submit an EU scheme registration without the electronic-interface flag set
    Then the system rejects the submission with validation error "MISSING_DECLARATION: electronic_interface"

  Scenario: EU scheme registration accepted for non-EU-established applicant with declaration
    Given a taxable person is not established in the EU
    And they submit an EU scheme registration with the "not_established_in_eu" declaration set to true
    And all other mandatory fields from §117 stk. 1 are present
    When the submission is processed
    Then the registration is accepted with status "PENDING_VAT_NUMBER"

  # ── FR-OSS-02-016 ──────────────────────────────────────────────────────────

  Scenario: EU scheme applicant with existing Danish VAT number uses that number
    Given a taxable person is already VAT-registered in Denmark with number "DK12345678"
    When they successfully register for the EU scheme
    Then no new VAT number is assigned
    And the EU scheme registration is linked to "DK12345678"
    And the registration status is "ACTIVE"

  # ── FR-OSS-02-017 ──────────────────────────────────────────────────────────

  Scenario: EU scheme applicant without Danish VAT registration receives new number within 8 days
    Given a taxable person is not VAT-registered in Denmark under ordinary rules
    And their complete EU scheme registration information is received on 2024-06-03
    When 7 calendar days have elapsed since receipt
    Then the system has assigned a unique VAT registration number
    And the number is communicated electronically to the taxable person
    And the registration status is "ACTIVE"

  # ── FR-OSS-02-018 ──────────────────────────────────────────────────────────

  Scenario: EU scheme number retained when ordinary Danish VAT registration ceases
    Given a taxable person is registered for the EU scheme using their Danish VAT number "DK12345678"
    When they cease their ordinary Danish VAT registration
    And they are not simultaneously deregistering from the EU scheme
    Then the system assigns a new individual EU scheme VAT number to replace "DK12345678"
    And the EU scheme registration remains ACTIVE


Feature: EU scheme identification member state binding rule
  As Skatteforvaltningen
  I want the EU scheme binding rule enforced automatically
  So that taxable persons cannot change identification member state in violation of ML § 66d stk. 2

  # ── FR-OSS-02-020 through FR-OSS-02-022 ────────────────────────────────────

  Scenario: Binding period end date set correctly on EU scheme registration under case b or c
    Given a taxable person registers for the EU scheme in calendar year 2024
    And they selected Denmark as identification member state under ML § 66d stk. 1 case b
    When the registration is confirmed
    Then the binding period end date is 2026-12-31
    And the system stores the binding rule type as "ML_66D_STK2"

  Scenario: Identification member state change blocked during binding period
    Given a taxable person is bound to Denmark as EU scheme identification member state until 2026-12-31
    When they attempt to change identification member state to France on 2025-06-01
    Then the system rejects the change with reason "BOUND_TO_IDENTIFICATION_MEMBER_STATE_UNTIL: 2026-12-31"

  Scenario: Binding period does not apply under ML § 66d stk. 1 case a (home establishment in Denmark)
    Given a taxable person's home establishment is in Denmark (case a)
    When they register for the EU scheme
    Then no binding period is recorded
    And the binding rule type is "NOT_APPLICABLE"

  # ── FR-OSS-02-021 (permitted change) ───────────────────────────────────────

  Scenario: Binding rule does not block identification state change when Denmark ceases to qualify
    Given a taxable person is bound to Denmark as identification member state under the EU scheme until 2026-12-31
    And their home establishment moves to France on 2025-03-15
    When they notify Skatteforvaltningen of the change on 2025-04-10
    Then the system permits the identification member state change
    And the change is effective 2025-03-15
    And the binding period clock resets in France from 2025-03-15


Feature: Registration change notifications
  As a taxable person registered under the Non-EU or EU scheme
  I want to notify Skatteforvaltningen of changes to my registration information
  So that the registration remains accurate as required by ML §§ 66b stk. 6 and 66e stk. 5

  # ── FR-OSS-02-023 through FR-OSS-02-026 ────────────────────────────────────

  Scenario: Timely change notification accepted and applied
    Given a taxable person with an active Non-EU scheme registration
    And a change to their postal address occurred on 2024-03-20
    When they submit the change notification on 2024-04-08
    Then the system updates the postal address
    And the notification is recorded as timely

  Scenario: Late change notification flagged for compliance review
    Given a taxable person with an active Non-EU scheme registration
    And a change to their phone number occurred on 2024-03-20
    When they submit the change notification on 2024-04-12
    Then the system updates the phone number
    And the notification is flagged as "LATE_NOTIFICATION"
    And the flag is visible in the compliance review queue

  Scenario: Business cessation notification submitted within 10-day deadline
    Given a taxable person with an active EU scheme registration
    And their business eligible for the scheme ceased on 2024-05-15
    When they submit the cessation notification on 2024-06-10
    Then the system records the cessation notification as timely
    And the registration is moved to status "CESSATION_NOTIFIED"

  Scenario: EU scheme identification state change notified to both old and new member state
    Given a taxable person registered under the EU scheme with Denmark as identification member state
    And their home establishment moved to Sweden on 2024-04-05
    When they notify Skatteforvaltningen and Sweden of the change on 2024-05-08
    Then Denmark records the identification member state change effective 2024-04-05
    And the system confirms the outgoing notification to Sweden was dispatched
    And the taxable person's registration in Denmark is closed as of 2024-04-05


Feature: Voluntary deregistration from Non-EU and EU schemes
  As a taxable person registered under the Non-EU or EU scheme
  I want to voluntarily deregister
  So that I can cease using the scheme in an orderly manner per ML § 66i

  # ── FR-OSS-02-027 through FR-OSS-02-030 ────────────────────────────────────

  Scenario: Voluntary deregistration with timely notification is effective next quarter
    Given a taxable person has an active Non-EU scheme registration
    When they submit a voluntary deregistration notification on 2024-03-14
    Then the deregistration is recorded as timely (15 or more days before 2024-03-31)
    And the deregistration effective date is 2024-04-01

  Scenario: Deregistration notification submitted exactly 15 days before quarter end is timely
    Given a taxable person has an active EU scheme registration
    When they submit a voluntary deregistration notification on 2024-03-17
    Then the deregistration is recorded as timely (exactly 15 days before 2024-03-31)
    And the deregistration effective date is 2024-04-01

  Scenario: Late deregistration notification defers effective date by one full quarter
    Given a taxable person has an active Non-EU scheme registration
    When they submit a voluntary deregistration notification on 2024-03-20
    Then 2024-03-20 is fewer than 15 days before 2024-03-31
    And the deregistration effective date is deferred to 2024-07-01
    And the taxable person is notified of the revised effective date

  Scenario: Taxable person may continue making eligible supplies until deregistration effective date
    Given a taxable person submits a timely voluntary deregistration effective 2024-04-01
    When they make an eligible supply on 2024-03-25
    Then the system accepts the supply as covered by the scheme
    And no error is raised for the supply date

  Scenario: Re-registration permitted immediately after voluntary deregistration without penalty
    Given a taxable person voluntarily deregistered from the EU scheme effective 2024-04-01
    When they submit a new EU scheme registration notification on 2024-04-15
    Then the registration is accepted
    And no exclusion flag or re-entry block is applied based on the prior deregistration


Feature: Forced exclusion and deregistration
  As Skatteforvaltningen acting as identification member state
  I want to enforce forced exclusion of taxable persons who meet exclusion criteria
  So that Denmark fulfils its obligations under ML § 66j and Momsforordningen artikel 58

  # ── FR-OSS-02-031 through FR-OSS-02-036 ────────────────────────────────────

  Scenario: Forced exclusion initiated on cessation notification (criterion 1)
    Given a taxable person with an active EU scheme registration
    When they notify Skatteforvaltningen on 2024-04-10 that they no longer make eligible supplies
    Then the system initiates forced exclusion for criterion "CESSATION_NOTIFICATION"
    And the exclusion decision is sent electronically on 2024-04-10
    And the exclusion effective date is 2024-07-01

  Scenario: Presumed cessation after 2 years triggers forced exclusion (criterion 2)
    Given a taxable person registered under the Non-EU scheme
    And they have reported no eligible supplies in any consumption member state since 2022-01-01
    When the system evaluates the registration on 2024-01-02
    Then the system detects presumed cessation after 2 full calendar years
    And the system initiates forced exclusion for criterion "PRESUMED_CESSATION"
    And the exclusion effective date is the first day of the quarter following the decision date

  Scenario: No forced exclusion triggered before 2-year presumed cessation threshold
    Given a taxable person registered under the EU scheme
    And they have reported no eligible supplies since 2023-06-01
    When the system evaluates the registration on 2024-06-01
    Then no presumed cessation exclusion is initiated
    And the registration remains ACTIVE

  Scenario: Forced exclusion for conditions no longer met (criterion 3)
    Given Skatteforvaltningen determines on 2024-09-01 that a taxable person no longer meets Non-EU scheme conditions
    When Skatteforvaltningen records the exclusion decision
    Then the exclusion effective date is 2024-10-01
    And the taxable person is notified electronically on 2024-09-01

  Scenario: Forced exclusion for persistent non-compliance imposes 2-year re-registration block (criterion 4)
    Given Skatteforvaltningen determines on 2024-06-15 that a taxable person has persistently failed to comply with EU scheme rules
    When the forced exclusion decision for "PERSISTENT_NON_COMPLIANCE" is recorded
    Then the exclusion effective date is 2024-07-01
    And a re-registration block is set from 2024-07-01 to 2026-06-30
    And any re-registration attempt before 2026-07-01 is rejected with reason "EXCLUDED_PERSISTENT_NON_COMPLIANCE_BLOCK_UNTIL: 2026-06-30"

  Scenario: 2-year block does not apply to exclusion for criterion 1 (cessation notification)
    Given a taxable person is excluded for criterion "CESSATION_NOTIFICATION" effective 2024-07-01
    When they attempt to re-register on 2024-10-01
    Then the re-registration is accepted
    And no 2-year block is applied

  Scenario: Forced exclusion effective date is date of establishment change, not next quarter
    Given a taxable person registered under the EU scheme
    And their home establishment moved from Denmark to France on 2024-05-20
    When Skatteforvaltningen records forced exclusion due to the establishment change
    Then the exclusion effective date is 2024-05-20
    And the standard next-quarter rule is not applied

  Scenario: Only Skatteforvaltningen can trigger forced exclusion
    Given a taxable person with an active Non-EU scheme registration
    When an actor other than Skatteforvaltningen attempts to trigger a forced exclusion
    Then the system rejects the action with reason "UNAUTHORISED_EXCLUSION_ACTOR"

  Scenario: Post-exclusion VAT obligations must be settled with consumption member states directly
    Given a taxable person has been excluded from the EU scheme effective 2024-07-01
    When they make an eligible supply in Germany on 2024-07-05
    Then the system does not accept the supply under the EU scheme registration
    And the system records that the taxable person must settle German VAT directly with German tax authorities


Feature: Scheme switching between Non-EU and EU schemes
  As a taxable person whose establishment status changes
  I want the system to handle scheme switching without a gap or overlap period
  So that my VAT obligations are continuously covered

  # ── FR-OSS-02-037 ──────────────────────────────────────────────────────────

  Scenario: Immediate switch from EU scheme to Non-EU scheme on establishment change
    Given a taxable person registered under the EU scheme with Denmark as identification member state
    And their last EU fixed establishment closes on 2024-08-01
    And they now qualify for the Non-EU scheme
    When the system processes the establishment change effective 2024-08-01
    Then exclusion from the EU scheme is effective 2024-08-01
    And the Non-EU scheme registration becomes effective 2024-08-01
    And there is no gap period between the two scheme registrations

  Scenario: Immediate switch from Non-EU scheme to EU scheme when EU establishment gained
    Given a taxable person registered under the Non-EU scheme
    And they establish a fixed place of business in Denmark on 2024-09-15
    When the system processes the new establishment effective 2024-09-15
    Then exclusion from the Non-EU scheme is effective 2024-09-15
    And a new EU scheme registration may commence from 2024-09-15


Feature: Transitional provision for pre-July-2021 registrations
  As Skatteforvaltningen
  I want pre-July-2021 registrations flagged for mandatory identification update
  So that all registration records comply with the rules in force from 1 July 2021

  # ── FR-OSS-02-038 ──────────────────────────────────────────────────────────

  Scenario: Pre-July-2021 registration flagged as overdue for update after 1 April 2022
    Given a taxable person was registered in the Non-EU scheme before 2021-07-01
    And they have not submitted an identification update under the new rules
    When the current date is 2022-04-02
    Then the registration is flagged as "TRANSITIONAL_UPDATE_OVERDUE"
    And the flag is visible in the compliance review queue

  Scenario: Pre-July-2021 registration flag cleared when identification update submitted
    Given a taxable person's registration is flagged as "TRANSITIONAL_UPDATE_OVERDUE"
    When they submit a complete identification update
    Then the flag is cleared
    And the registration record shows the update date

  Scenario: New return period blocked for overdue transitional registrant
    Given a taxable person's registration is flagged as "TRANSITIONAL_UPDATE_OVERDUE"
    When the system attempts to open a new quarterly return period for the taxable person
    Then the return period is not opened
    And the block reason is "TRANSITIONAL_UPDATE_REQUIRED"

  Scenario: Pre-July-2021 registration not yet flagged before 1 April 2022
    Given a taxable person was registered in the EU scheme before 2021-07-01
    And they have not submitted an identification update
    When the current date is 2022-03-31
    Then the registration is NOT flagged as "TRANSITIONAL_UPDATE_OVERDUE"
