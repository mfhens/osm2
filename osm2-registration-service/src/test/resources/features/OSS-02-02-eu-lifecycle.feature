# OSS-02 — Registrering og afmeldelse
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

