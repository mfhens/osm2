# OSS-02 — Registrering og afmeldelse
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

