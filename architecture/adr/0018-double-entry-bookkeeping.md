# ADR-0018: Double-Entry Bookkeeping for Payment Accounting

**Status**: Accepted
**Date**: 2025-07-01
**Deciders**: Architecture Team

## Context

EU OSS payment processing requires distributable payment accounting across multiple member states. A single taxable person's quarterly OSS return may generate payment obligations to 27 EU member states simultaneously, with amounts in different currencies requiring ECB exchange rate conversion. A single-entry ledger model (recording only the inbound payment amount) is insufficient for financial audit, reconciliation, and the formal accounting obligations that arise from acting as an OSS intermediary for Skatteforvaltningen.

## Decision

`osm2-payment-service` implements double-entry bookkeeping in-house within the `bookkeeping` package.

Every payment event is recorded as a balanced journal entry — the sum of debits equals the sum of credits for every transaction. The key journal entry types are:

| Event | Debit | Credit |
|---|---|---|
| Payment receipt | Bank/clearing account | Taxable person liability account |
| Member-state distribution | Member-state payable | Bank/clearing account |
| Refund | Taxable person liability | Bank/clearing account |
| FX conversion | Target currency account | Source currency account + FX gain/loss |

ECB exchange rate conversions are recorded as explicit FX journal entries with the rate, source currency, target currency, and conversion date. The ECB rate is fetched from the ECB reference rate feed and cached per business day.

The bookkeeping ledger is stored in the payment-service PostgreSQL schema (ADR-0011) as the primary record. All journal entries are additionally appended to immudb (ADR-0029) to provide a tamper-evident financial audit trail with cryptographic proof of inclusion.

## Consequences

**Positive**
- Full financial audit trail: every euro of an OSS payment is traceable from receipt through member-state distribution.
- Reconciliation queries are possible directly against the ledger without requiring external accounting tools.
- Double-entry invariant (debits = credits) acts as a built-in consistency check — an imbalanced entry is a code defect caught at runtime.
- FX conversion is explicit and auditable: the rate used for each conversion is recorded at the time of the transaction.

**Negative**
- The custom bookkeeping implementation must be maintained by the team. Changes to the double-entry model require careful review to preserve the debit=credit invariant.
- Development and operations teams must understand double-entry accounting concepts to maintain the ledger schema and diagnose reconciliation issues.
- Journal entry volume is higher than a simple ledger: a single OSS payment touching 10 member states generates at minimum 21 journal entries (1 receipt + 10 distributions × 2 sides each).
