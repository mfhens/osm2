# OSS-02 — Registrering og afmeldelse
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

