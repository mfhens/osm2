# OSS-05 — Outcome Contract

## Petition Reference

| Field       | Value                                      |
|-------------|--------------------------------------------|
| Petition ID | OSS-05                                     |
| Title       | Betaling, refusion og rykkere              |
| Date        | 2026-03-30                                 |

---

## 1. Definition of Done

OSS-05 is complete when all acceptance criteria below pass, all Gherkin scenarios in `OSS-05.feature` are implemented and green, and no acceptance criterion is marked as pending.

---

## 2. Acceptance Criteria

### AC-01 — Payment deadline enforcement

**Given** a filed OSS return with a known deadline  
**Then** the system accepts a payment received at or before the deadline as on time  
**And** the system marks a payment received after the deadline as late  
**And** no separate, later payment deadline exists apart from the return deadline

**Measurable:** System timestamp of payment receipt is compared against the return filing deadline. Zero tolerance: one second late is late.

---

### AC-02 — DKK currency enforcement

**Given** a payment submitted in a currency other than DKK  
**Then** the system rejects or flags the payment before crediting it to the return  
**And** the payment is not applied to any outstanding liability until it has been converted to DKK

**Measurable:** 100% of payments in non-DKK currency are flagged before ledger credit. Zero DKK-denominated liabilities are reduced by a non-DKK payment without conversion.

---

### AC-03 — OSS bank account routing

**Given** an OSS payment is due  
**Then** the system directs the payer to Skatteforvaltningen's designated OSS bank account  
**And** the payment is not routed through the general single-tax-account (én skattekonto)

**Measurable:** All OSS payments reference only the designated OSS account identifier in payment instructions.

---

### AC-04 — Period-specific payment reference

**Given** a payment is received  
**Then** the system validates that it carries a valid return reference number  
**And** it is credited only to that specific return  
**And** subsequent adjustments to that payment cannot be attributed to a different return period

**Measurable:** Attempting to attribute a payment to a return other than its declared reference produces a validation error. Zero cross-period attributions in the audit log.

---

### AC-05 — ECB rate at period end (critical)

**Given** a return period ending on date D  
**And** the return includes amounts in a foreign currency  
**When** the payment amount is validated or converted  
**Then** the system uses the ECB exchange rate published on date D  
**And** the system does not use the ECB rate at the date the payment is received

**Measurable:** For any given return period, the exchange rate applied to the payment equals the ECB rate on the last day of that period, regardless of when payment arrives. This can be verified by comparison against the rate used in the corresponding return (OSS-04). Any divergence between return rate and payment rate for the same period is a defect.

---

### AC-06 — Overpayment refund — IMS direct

**Given** a payment received exceeds the VAT declared on the associated return  
**And** Denmark (IMS) has not yet distributed the funds to any CMS  
**Then** the system triggers a refund of the excess amount to the taxable person or intermediary  
**And** the refund amount equals the difference between payment received and declared VAT

**Measurable:** Refund amount = payment received − declared VAT, when no distribution has occurred. Refund is initiated within the processing cycle for that return.

---

### AC-07 — Overpayment refund — CMS coordination

**Given** Denmark has already distributed funds for a return period to one or more CMS  
**And** it is subsequently established that the payment was in excess of the declared return  
**Then** each CMS is notified of the excess and their share to refund directly to the taxable person or intermediary  
**And** each CMS notifies Denmark electronically of the refund amount it has issued  
**And** an audit record is created for each inter-state notification exchanged

**Measurable:** For each overpayment coordination event: (a) every affected CMS receives a notification, (b) Denmark's system records receipt of each CMS's electronic refund confirmation, (c) the sum of CMS refund confirmations equals the total overpayment minus any amount Denmark refunded directly.

---

### AC-08 — Day-10 reminder dispatch

**Given** a return has been filed  
**And** the payment deadline has passed  
**And** payment is absent or less than the declared amount  
**When** the 10th day after the payment deadline is reached  
**Then** Denmark sends an electronic reminder to the taxable person or intermediary  
**And** Denmark simultaneously notifies each relevant CMS electronically that the reminder was sent  
**And** no reminder is sent before day 10  
**And** sending the same reminder twice for the same return period has no additional effect (idempotent)

**Measurable:** Reminder dispatch timestamp falls on day 10 (± system processing latency within the same calendar day). CMS notification and taxpayer reminder are dispatched in the same processing event. Duplicate dispatch attempts produce no second outbound message.

---

### AC-09 — Enforcement handover to CMS

**Given** Denmark has sent the Day-10 reminder for a return period  
**Then** the system records the enforcement handover date for each CMS  
**And** Denmark's role in that payment period is set to "reminder sent — enforcement transferred"  
**And** the system does not send further reminders to the taxable person on behalf of Denmark for that period

**Measurable:** After the Day-10 reminder is recorded, no further Denmark-originated reminder is generated for that (return period, CMS) combination.

---

### AC-10 — Payment routing after CMS reminder

**Given** a CMS has sent its own reminder for outstanding VAT  
**And** Denmark's system has received the CMS's electronic notification of that reminder  
**Then** the outstanding VAT for that CMS's share is marked as payable directly to the CMS  
**And** the amount is no longer routable through Denmark's OSS payment account

**Measurable:** After CMS notification is recorded, attempting to submit payment via Denmark for the CMS's share produces a routing rejection or redirect instruction.

---

### AC-11 — Import scheme joint and several liability

**Given** a return filed under the Import scheme with an intermediary  
**Then** the system records both the supplier (taxable person) and the intermediary as co-obligors  
**And** a payment from either party fully or partially discharges the shared liability  
**And** enforcement action can be initiated against either party independently  
**And** the liability record shows both parties for the duration of the obligation

**Measurable:** (a) Both obligors are present in the liability record. (b) A payment credited from one party reduces the balance visible to both. (c) An enforcement event against one party does not remove the other party from the liability record.

---

### AC-12 — Penalties, interest, and costs — CMS-only jurisdiction

**Given** a late, absent, or incorrect payment has been identified  
**Then** the system records that penalties, interest, and costs are the exclusive jurisdiction of the relevant CMS  
**And** the system does not calculate, assess, or collect penalty amounts on behalf of any CMS  
**And** Denmark does not receive penalty amounts in its OSS bank account

**Measurable:** Zero penalty or interest amounts are posted to Denmark's OSS ledger. The system's payment routing confirms that penalty payments are directed to CMS, not Denmark.

---

### AC-13 — Distribution to consumption member states

**Given** a payment has been received and matched to a return  
**Then** the system calculates each CMS's distribution share based on the filed return  
**And** the system records the distribution amount per CMS per return period  
**And** the distribution status (pending, forwarded, confirmed) is tracked per CMS

**Measurable:** Sum of all CMS distribution amounts equals total VAT distributed (may differ from total collected due to overpayment handling). Each CMS distribution record has a status and a timestamp.

---

## 3. Failure Conditions

The following constitute defects that must be resolved before OSS-05 is considered done:

| ID   | Failure condition                                                                                 |
|------|---------------------------------------------------------------------------------------------------|
| F-01 | Any payment is converted using an exchange rate other than the ECB rate at period end             |
| F-02 | A payment in a currency other than DKK is credited to a liability without conversion              |
| F-03 | A Day-10 reminder is dispatched before day 10 or is not dispatched by end of day 10              |
| F-04 | Enforcement handover is not recorded after Day-10 reminder is sent                               |
| F-05 | Only the intermediary (and not also the supplier) is recorded as obligor in the Import scheme     |
| F-06 | A penalty or interest amount is posted to Denmark's OSS ledger                                    |
| F-07 | A payment is attributed to a return period other than the one on its reference                    |
| F-08 | An overpayment refund coordination event lacks an audit trail of CMS notifications                |
| F-09 | Payment for a CMS's share is routed through Denmark after that CMS has sent its own reminder     |

---

## 4. Traceability

| Acceptance Criterion | Petition FR / NFR | Gherkin Scenario(s)                        |
|----------------------|-------------------|--------------------------------------------|
| AC-01                | FR-01             | SC-01                                      |
| AC-02                | FR-02             | SC-02                                      |
| AC-03                | FR-03             | SC-03                                      |
| AC-04                | FR-04             | SC-04                                      |
| AC-05                | FR-05             | SC-05, SC-06                               |
| AC-06                | FR-06             | SC-07                                      |
| AC-07                | FR-07             | SC-08, SC-09                               |
| AC-08                | FR-08, NFR-03     | SC-10, SC-11, SC-12                        |
| AC-09                | FR-09             | SC-13                                      |
| AC-10                | FR-10             | SC-14                                      |
| AC-11                | FR-11             | SC-15, SC-16                               |
| AC-12                | FR-12             | SC-17                                      |
| AC-13                | FR-13             | SC-18                                      |
