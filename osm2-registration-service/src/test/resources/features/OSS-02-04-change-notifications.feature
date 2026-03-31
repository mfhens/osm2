# OSS-02 — Registrering og afmeldelse
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

