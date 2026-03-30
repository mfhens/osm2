# language: en
Feature: OSS-05 — Betaling, refusion og rykkere
  As Skatteforvaltningen acting as identification member state (IMS)
  I need to process VAT payments for all three OSS special arrangement schemes (Union, Non-Union, Import)
  So that VAT collected from taxable persons and intermediaries is correctly distributed to consumption member states,
  overpayments are refunded, late payments trigger the prescribed reminder and enforcement handover, and
  the Import scheme's joint and several liability is properly enforced.

  Background:
    Given Denmark is acting as the identification member state (IMS) for OSS

  # ---------------------------------------------------------------------------
  # Payment deadline
  # ---------------------------------------------------------------------------

  Scenario: SC-01 — On-time payment accepted when received at or before return deadline
    Given a taxable person has filed a Union scheme VAT return for period "Q1-2026"
    And the return filing deadline for "Q1-2026" is "2026-04-30T23:59:59"
    When a payment of 12500.00 DKK referencing return "Q1-2026" is received at "2026-04-30T18:00:00"
    Then the payment is recorded as on time
    And the outstanding liability for return "Q1-2026" is reduced by 12500.00 DKK

  Scenario: SC-02 — Late payment flagged when received after return deadline
    Given a taxable person has filed a Union scheme VAT return for period "Q1-2026"
    And the return filing deadline for "Q1-2026" is "2026-04-30T23:59:59"
    When a payment of 12500.00 DKK referencing return "Q1-2026" is received at "2026-05-01T00:00:01"
    Then the payment is recorded as late
    And the outstanding liability for return "Q1-2026" remains subject to late-payment processing

  # ---------------------------------------------------------------------------
  # Currency enforcement
  # ---------------------------------------------------------------------------

  Scenario: SC-03 — Payment in DKK is accepted without conversion
    Given a Non-Union scheme VAT return for period "Q1-2026" is outstanding
    When a payment of 8000.00 DKK is submitted
    Then the system accepts the payment without currency conversion
    And the liability is reduced by 8000.00 DKK

  Scenario: SC-04 — Payment in foreign currency is rejected before ledger credit
    Given a Non-Union scheme VAT return for period "Q1-2026" is outstanding
    When a payment of 1000.00 EUR is submitted
    Then the system rejects or flags the payment before any ledger credit
    And the outstanding liability is not reduced until the amount is properly converted to DKK

  # ---------------------------------------------------------------------------
  # OSS bank account routing
  # ---------------------------------------------------------------------------

  Scenario: SC-05 — Payment instructions reference the designated OSS bank account
    Given a taxable person owes VAT for period "Q1-2026"
    When the system generates payment instructions
    Then the payment instructions reference Skatteforvaltningen's designated OSS bank account
    And the payment instructions do not reference the general single-tax-account (én skattekonto)

  # ---------------------------------------------------------------------------
  # Period-specific payment reference
  # ---------------------------------------------------------------------------

  Scenario: SC-06 — Payment without a valid return reference is rejected
    Given a Union scheme return for "Q1-2026" is outstanding
    When a payment arrives with no return reference number
    Then the system rejects the payment with a validation error indicating a missing return reference

  Scenario: SC-07 — Payment cannot be attributed to a different return period
    Given a payment was received referencing return "Q1-2026"
    When an operator attempts to re-attribute the payment to return "Q4-2025"
    Then the system rejects the re-attribution with a validation error
    And the payment remains attributed to return "Q1-2026"

  # ---------------------------------------------------------------------------
  # Currency conversion — ECB rate at last day of return period (CRITICAL)
  # ---------------------------------------------------------------------------

  Scenario: SC-08 — Currency conversion uses ECB rate at last day of return period, not at payment date
    Given a Union scheme return for period "Q1-2026" ending on "2026-03-31"
    And the ECB exchange rate for EUR on "2026-03-31" is 7.4601 DKK/EUR
    And the ECB exchange rate for EUR on "2026-04-15" is 7.5100 DKK/EUR
    And the declared VAT amount is 2000.00 EUR
    When a payment is received on "2026-04-15"
    Then the system converts the declared amount using the rate on "2026-03-31" (7.4601 DKK/EUR)
    And the converted amount is 14920.20 DKK
    And the rate on "2026-04-15" (7.5100 DKK/EUR) is not used

  Scenario: SC-09 — Payment ECB rate matches the rate used in the corresponding return
    Given a Union scheme return for period "Q1-2026" ending on "2026-03-31"
    And the return was filed using ECB rate 7.4601 DKK/EUR for "2026-03-31"
    When the payment amount in DKK is validated
    Then the exchange rate applied to the payment equals the exchange rate recorded on the return
    And the difference between the return's DKK amount and the payment's DKK amount is zero for exchange rate reasons

  # ---------------------------------------------------------------------------
  # Overpayment refund — IMS refunds directly
  # ---------------------------------------------------------------------------

  Scenario: SC-10 — Overpayment refunded directly by Denmark when funds not yet distributed
    Given a Union scheme return for "Q1-2026" declares VAT of 10000.00 DKK
    And Denmark has not yet forwarded funds for "Q1-2026" to any consumption member state
    When a payment of 12000.00 DKK referencing return "Q1-2026" is received
    Then the system identifies an overpayment of 2000.00 DKK
    And Denmark initiates a direct refund of 2000.00 DKK to the taxable person or intermediary
    And no distribution to consumption member states includes the excess 2000.00 DKK

  # ---------------------------------------------------------------------------
  # Overpayment refund — CMS coordination protocol
  # ---------------------------------------------------------------------------

  Scenario: SC-11 — CMS coordination initiated when funds have already been distributed
    Given a Union scheme return for "Q1-2026" declared VAT of 10000.00 DKK
    And Denmark has already distributed funds for "Q1-2026" to consumption member states:
      | CMS     | Amount (DKK) |
      | Germany | 6000.00      |
      | France  | 4000.00      |
    When it is established that the payment of 10000.00 DKK was in excess by 1000.00 DKK
    Then Denmark notifies Germany electronically of an overpayment share of 600.00 DKK to refund
    And Denmark notifies France electronically of an overpayment share of 400.00 DKK to refund
    And an audit record is created for each outgoing CMS notification

  Scenario: SC-12 — CMS notifies Denmark of refund amount issued
    Given Denmark has initiated overpayment coordination with Germany for return "Q1-2026"
    When Germany sends an electronic notification to Denmark confirming a refund of 600.00 DKK to the taxable person
    Then Denmark records receipt of Germany's refund confirmation of 600.00 DKK
    And the audit trail for "Q1-2026" includes Germany's electronic notification

  # ---------------------------------------------------------------------------
  # Late or absent payment — Day-10 reminder
  # ---------------------------------------------------------------------------

  Scenario: SC-13 — Day-10 reminder sent on exactly the 10th day after payment deadline
    Given a Union scheme return for "Q1-2026" has been filed
    And the payment deadline was "2026-04-30"
    And no payment has been received for return "Q1-2026" by end of "2026-04-30"
    When the date reaches "2026-05-10"
    Then Denmark sends an electronic reminder to the taxable person referencing return "Q1-2026"
    And Denmark simultaneously sends an electronic notification to each relevant consumption member state that a reminder was sent

  Scenario: SC-14 — No reminder is sent before the 10th day
    Given a Union scheme return for "Q1-2026" has been filed
    And the payment deadline was "2026-04-30"
    And no payment has been received for return "Q1-2026"
    When the date is "2026-05-09"
    Then Denmark has not yet sent a reminder for return "Q1-2026"

  Scenario: SC-15 — Duplicate reminder dispatch is idempotent
    Given Denmark has already sent a Day-10 reminder for return "Q1-2026" on "2026-05-10"
    When the system attempts to send a second Day-10 reminder for return "Q1-2026"
    Then no additional outbound reminder message is generated
    And the audit log records only one reminder event for return "Q1-2026"

  # ---------------------------------------------------------------------------
  # Enforcement handover to CMS
  # ---------------------------------------------------------------------------

  Scenario: SC-16 — Enforcement transferred to CMS after Day-10 reminder
    Given Denmark has sent the Day-10 reminder for return "Q1-2026" on "2026-05-10"
    Then the system records the enforcement handover date "2026-05-10" for each relevant CMS
    And Denmark's status for return "Q1-2026" is set to "reminder sent — enforcement transferred"
    And Denmark does not send further reminders for return "Q1-2026"

  # ---------------------------------------------------------------------------
  # Payment routing after CMS sends its own reminder
  # ---------------------------------------------------------------------------

  Scenario: SC-17 — Outstanding VAT routed directly to CMS after CMS sends reminder
    Given Denmark has sent the Day-10 reminder for return "Q1-2026"
    And Germany sends Denmark an electronic notification that Germany has issued its own reminder to the taxable person
    When Denmark records Germany's reminder notification for return "Q1-2026"
    Then Germany's share of the outstanding VAT for "Q1-2026" is marked as payable directly to Germany
    And that amount is no longer payable through Denmark's OSS bank account

  Scenario: SC-18 — Attempting to pay CMS share through Denmark after CMS reminder produces a routing rejection
    Given Germany's share for return "Q1-2026" is marked as payable directly to Germany
    When the taxable person attempts to submit Germany's share via Denmark's OSS bank account
    Then the system rejects or redirects the payment with an instruction to pay directly to Germany

  # ---------------------------------------------------------------------------
  # Import scheme joint and several liability
  # ---------------------------------------------------------------------------

  Scenario: SC-19 — Both supplier and intermediary recorded as co-obligors in Import scheme
    Given a taxable person "Supplier A" uses the Import scheme via intermediary "Intermediary B"
    When an Import scheme return for period "Q1-2026" is filed by "Intermediary B"
    Then the liability record for return "Q1-2026" includes "Supplier A" as co-obligor
    And the liability record for return "Q1-2026" includes "Intermediary B" as co-obligor

  Scenario: SC-20 — Payment from intermediary discharges shared Import scheme liability
    Given the liability record for return "Q1-2026" shows co-obligors "Supplier A" and "Intermediary B" with 5000.00 DKK outstanding
    When "Intermediary B" pays 5000.00 DKK referencing return "Q1-2026"
    Then the outstanding balance is 0.00 DKK
    And "Supplier A" remains a recorded co-obligor until the obligation period is closed

  Scenario: SC-21 — Enforcement may be initiated against supplier independently of intermediary
    Given the liability record for return "Q1-2026" shows co-obligors "Supplier A" and "Intermediary B" with 5000.00 DKK outstanding
    When enforcement action is initiated against "Supplier A"
    Then the enforcement record references return "Q1-2026" and "Supplier A"
    And "Intermediary B" remains a co-obligor on the same liability

  # ---------------------------------------------------------------------------
  # Penalties, interest, and costs — CMS jurisdiction
  # ---------------------------------------------------------------------------

  Scenario: SC-22 — Penalties and interest are not posted to Denmark's OSS ledger
    Given Germany has assessed a late-payment penalty of 300.00 EUR against the taxable person for return "Q1-2026"
    Then Denmark's OSS ledger contains no entry for that penalty amount
    And the system records that the penalty is payable directly to Germany
    And Denmark does not receive the penalty amount

  # ---------------------------------------------------------------------------
  # Distribution to consumption member states
  # ---------------------------------------------------------------------------

  Scenario: SC-23 — Distribution amounts calculated per CMS from the filed return
    Given a Union scheme return for "Q1-2026" has been received with the following CMS allocations:
      | CMS     | VAT Amount (DKK) |
      | Germany | 6000.00          |
      | France  | 3000.00          |
      | Sweden  | 1000.00          |
    When the payment of 10000.00 DKK is matched and accepted
    Then the system creates a distribution record for Germany of 6000.00 DKK with status "pending"
    And the system creates a distribution record for France of 3000.00 DKK with status "pending"
    And the system creates a distribution record for Sweden of 1000.00 DKK with status "pending"

  Scenario: SC-24 — Distribution status transitions from pending to forwarded
    Given a distribution record for Germany of 6000.00 DKK for "Q1-2026" is in status "pending"
    When Denmark forwards 6000.00 DKK to Germany
    Then the distribution record for Germany is updated to status "forwarded"
    And a timestamp is recorded on the forwarded distribution record
