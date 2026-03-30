# OSS-05 — Betaling, refusion og rykkere

## Status

| Field          | Value                                      |
|----------------|--------------------------------------------|
| Petition ID    | OSS-05                                     |
| Title          | Betaling, refusion og rykkere              |
| Status         | Submitted                                  |
| Author         | osm2 programme                             |
| Date           | 2026-03-30                                 |
| Supersedes     | —                                          |
| Superseded by  | —                                          |

---

## 1. Summary

Implement the payment processing module for all three OSS special arrangement schemes (Union, Non-Union, Import). The module handles the full payment lifecycle: timely payment of VAT due, distribution of collected amounts to consumption member states, refund of overpayments, issuance of Day-10 late-payment reminders, and handover of enforcement responsibility to the relevant consumption member states. For the Import scheme, the module must enforce the joint and several liability (solidarisk hæftelse) between the taxable supplier and the intermediary.

---

## 2. Context and Motivation

osm2 implements Denmark's role as identification member state (IMS) for the EU One-Stop-Shop VAT special arrangements under ML §§ 66–66u. When Denmark is the IMS, taxable persons and intermediaries remit VAT collected across all EU consumption member states (CMS) to Skatteforvaltningen in a single payment. Skatteforvaltningen then distributes each CMS's share.

OSS-04 covers the VAT return filing workflow. OSS-05 covers the subsequent, tightly coupled payment workflow. The two deadlines coincide by law: payment is due no later than the return filing deadline (ML § 66g stk. 1; ML § 66p stk. 3). A correct, functioning payment module is a prerequisite for Denmark to fulfil its obligations as IMS under EU law and to avoid infringement proceedings.

Key legal risk: currency conversion must use the ECB exchange rate published on the **last day of the return period**, not the rate on the day payment is received. This is the same rule that applies to the return itself (D.A.16.3.6.1.2). Applying the wrong rate is a common implementation error and will cause systematic discrepancies between return amounts and payment amounts.

---

## 3. Legal Basis

| Instrument                                  | Provision                                  | Subject                                                       |
|---------------------------------------------|--------------------------------------------|---------------------------------------------------------------|
| Momslov (ML)                                | § 66g stk. 1                               | Payment deadline — Union and Non-Union schemes                |
| Momslov (ML)                                | § 66p stk. 3                               | Payment deadline — Import scheme                              |
| Momslov (ML)                                | § 66m                                      | Intermediary liability — Import scheme                        |
| Momslov (ML)                                | § 46 stk. 1, 1. pkt.                       | General taxpayer liability (applies to supplier in import)    |
| Momssystemdirektivet (MSD)                  | Art. 367                                   | Payment — Union scheme                                        |
| Momssystemdirektivet (MSD)                  | Art. 369i                                  | Payment — Non-Union scheme                                    |
| Momssystemdirektivet (MSD)                  | Art. 369v                                  | Payment — Import scheme                                       |
| Momssystemdirektivet (MSD)                  | Art. 369l                                  | Joint liability — Import scheme intermediary                  |
| Momssystemdirektivet (MSD)                  | Art. 193                                   | General taxpayer liability                                    |
| Momsforordningen (EU) 282/2011              | Art. 62 (as amended by 2019/2026)          | Period-specific payment reference; adjustment rules           |
| Momsforordningen (EU) 282/2011              | Art. 63 (as amended by 2019/2026)          | Overpayment refund — IMS and CMS procedures                   |
| Momsforordningen (EU) 282/2011              | Art. 63a (as amended by 2019/2026)         | Late/no payment — reminder and enforcement handover           |
| Momsforordningen (EU) 282/2011              | Art. 63b (as amended by 2019/2026)         | Penalties, interest, and costs — CMS responsibility           |
| Momsbekendtgørelsen                         | § 120 stk. 6, stk. 11                      | DKK payment currency; bank account for Union scheme           |
| Momsbekendtgørelsen                         | § 121 stk. 3, stk. 8                       | DKK payment currency; bank account for Import scheme          |
| Gennemførelsesforordning (EU) 2019/2026     | —                                          | Amends Arts. 62, 63, 63a, 63b of momsforordningen            |
| Direktiv (EU) 2017/2455                     | Art. 2                                     | Amends MSD payment articles                                   |
| Direktiv (EU) 2019/1995                     | —                                          | Further amends Import scheme provisions                       |

**Reference document:** `docs/references/DA16.3.7-betaling.md` (Den juridiske vejledning 2026-1, D.A.16.3.7)

---

## 4. Actors

| Actor                          | Role                                                                                      |
|--------------------------------|-------------------------------------------------------------------------------------------|
| Taxable person                 | Entity registered under Union or Non-Union scheme; primary payment obligor                |
| Intermediary (formidler)       | Entity registered under Import scheme on behalf of a supplier; jointly liable for VAT     |
| Supplier                       | Underlying taxable person in Import scheme; also jointly and severally liable             |
| Skatteforvaltningen (Denmark IMS) | Receives payments, issues Day-10 reminders, distributes to CMS, refunds overpayments  |
| Consumption member state (CMS) | Receives its share of VAT from IMS; responsible for post-reminder enforcement; refunds overpayment shares |
| ECB                            | Publishes the exchange rate used for currency conversion                                   |

---

## 5. Functional Requirements

### FR-01 — Payment deadline

The system must enforce that VAT payment is due simultaneously with the return filing deadline. A payment is on time if it is received no later than the moment the return filing period expires. Payment is not a separate, later deadline.

### FR-02 — Payment currency (DKK)

All payments from taxable persons and intermediaries to Denmark (as IMS) must be received in Danish kroner (DKK). The system must reject or flag payments made in a foreign currency at the point of receipt.

### FR-03 — Payment bank account

The system must direct payers to Skatteforvaltningen's designated bank account for OSS payments. This account is communicated by Skatteforvaltningen. The system must not route OSS payments through the general single-tax-account (én skattekonto) mechanism.

### FR-04 — Period-specific payment reference

Each payment must carry the reference number of the specific VAT return it relates to. The system must:
- Validate that every incoming payment has a valid return reference.
- Prevent the same payment from being attributed to a different return period.
- Prevent subsequent adjustments from being posted to a different return.

### FR-05 — Currency conversion rule

When the return includes amounts in currencies other than DKK, the system must convert using the ECB exchange rate published on the **last day of the return period**. This is the same rate used for the return itself. The system must not use the exchange rate at the date of payment receipt.

### FR-06 — Overpayment refund — IMS refunds directly

When the total payment received for a return exceeds the VAT declared on that return, and Denmark (IMS) has not yet forwarded those funds to the CMS, Denmark must refund the excess amount directly to the taxable person or intermediary.

### FR-07 — Overpayment refund — CMS coordination protocol

When Denmark (IMS) has already distributed funds for a return period to the CMS, and it is subsequently established that the payment was in excess of the return amount, the CMS must each refund their respective share of the overpayment directly to the taxable person or intermediary. Each CMS must notify Denmark electronically of the refund amount it has issued.

### FR-08 — Late or absent payment — Day-10 reminder

When a return has been filed but payment is absent or less than the declared amount, and the payment deadline has passed, Denmark (IMS) must send an electronic reminder to the taxable person or intermediary on the **10th day after the payment deadline**. Denmark must simultaneously notify each relevant CMS electronically that a reminder has been sent.

### FR-09 — Enforcement handover to CMS

After Denmark has sent the Day-10 reminder (FR-08), all subsequent reminders and enforcement action become the sole responsibility of the relevant CMS. The system must:
- Record the date the Day-10 reminder was sent.
- Transfer the enforcement flag to each CMS.
- Lock Denmark's role in that payment period to "reminder sent — enforcement transferred".

### FR-10 — Payment routing after CMS reminder

When a CMS has subsequently issued its own reminder, the outstanding VAT for that CMS's share must be paid **directly to that CMS** — not to Denmark. The system must:
- Record when a CMS notifies Denmark that it has sent a reminder.
- Update the payment routing so that the amount due to that CMS is no longer payable via Denmark.

### FR-11 — Import scheme joint and several liability

In the Import scheme, both the taxable person (supplier) and the intermediary are jointly and severally liable for all VAT payable through the scheme. The system must:
- Record both the supplier and the intermediary as payment obligors for each import scheme return.
- Allow payment from either party and apply it to the liability.
- Permit enforcement action against either party independently.

### FR-12 — Penalties, interest, and costs — CMS jurisdiction

Interest, fines, penalties, and costs arising from late, absent, or incorrect payment are assessed and determined exclusively by the relevant CMS, applying CMS law. The taxable person or intermediary pays these amounts directly to the CMS — not to Denmark. Denmark must not assess, collect, or receive penalty payments on behalf of a CMS.

### FR-13 — Distribution to consumption member states

Denmark must forward each CMS's share of collected VAT to that CMS. Distribution amounts derive from the filed return. The system must record the distribution amounts and their payment status per CMS per return period.

---

## 6. Non-Functional Requirements

| ID    | Requirement                                                                                                   |
|-------|---------------------------------------------------------------------------------------------------------------|
| NFR-01 | All payment events (receipt, distribution, refund, reminder, enforcement handover) must be logged with timestamp, return reference, amount, currency, and actor. |
| NFR-02 | The ECB rate lookup must be deterministic: the same period must always return the same rate (sourced from the last day of the period). |
| NFR-03 | Reminder dispatch (FR-08) must be idempotent: sending the same reminder twice for the same return period must have no additional effect. |
| NFR-04 | The overpayment coordination protocol (FR-07) must produce an audit trail sufficient for inter-state reconciliation. |

---

## 7. Constraints and Assumptions

- Denmark is acting as identification member state. OSS-05 does not cover Denmark's role as a consumption member state.
- The ECB rate source and retrieval mechanism are assumed to be provided by a shared infrastructure component; OSS-05 requires that the rate used is the correct one (last day of period), but does not implement the ECB integration itself.
- The bank account number for OSS payments is administered by Skatteforvaltningen outside the system; the system must reference a configurable account identifier.
- The one-time transition rule for pre-2019 periods (Reg. 904/2010 art. 46 stk. 3) is within scope for the overpayment refund logic but is expected to be invoked only for legacy data.
- OSS-05 does not implement the CMS-side logic for enforcement, penalties, or interest — only Denmark's obligations as IMS.

---

## 8. Out of Scope

| Item                                                           | Covered by  |
|----------------------------------------------------------------|-------------|
| VAT return filing and calculation                              | OSS-04      |
| Record-keeping obligations                                     | OSS-06      |
| Denmark's role as consumption member state (receiving from other IMS) | Future petition |
| ECB exchange rate data feed integration                        | Shared infrastructure |
| Penalty and interest calculation (CMS-side)                    | CMS systems — not osm2 |
| Registration and de-registration of taxable persons           | OSS-01/02/03 |
| Single tax account (én skattekonto) integration               | Not applicable to OSS payments |

---

## 9. Key Risks

| Risk                                          | Description                                                                                                           | Mitigation                                                                                   |
|-----------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------|
| Wrong ECB rate applied                        | Using the payment-receipt rate instead of the period-end rate causes systematic return/payment discrepancies          | FR-05 specifies the period-end rule explicitly; validation against return amount is required  |
| Overpayment coordination complexity           | The multi-state refund protocol (FR-07) involves asynchronous CMS notifications; errors create inter-state disputes   | Audit log (NFR-04); state machine for each refund coordination instance                      |
| Import joint liability not enforced           | Treating only the intermediary as liable breaks enforcement options against the supplier                               | FR-11 requires both parties recorded as obligors                                             |
| Enforcement handover timing error             | Day-10 reminder dispatched on wrong day, or enforcement not flagged, leaves Denmark with residual liability           | FR-08 and FR-09 must be implemented atomically                                               |
