# OSS-02 — Registrering og afmeldelse
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

