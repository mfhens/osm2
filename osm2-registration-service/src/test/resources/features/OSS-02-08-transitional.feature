# OSS-02 — Registrering og afmeldelse
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
