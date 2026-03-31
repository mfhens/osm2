# OSS-02 — Registrering og afmeldelse
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

