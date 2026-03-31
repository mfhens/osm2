# OSS-02 — Registrering og afmeldelse
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

