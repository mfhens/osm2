# ADR-0033 — Bitemporal data model for registration lifecycle

**Status:** Accepted  
**Date:** 2026-03-30  
**Deciders:** Platform team

---

## Context

The OSS VAT system manages registration lifecycles under three schemes (Ikke-EU, EU, Importordning). Legal analysis of the juridisk vejledning (Den juridiske vejledning, D.A.16) reveals that the registration domain has an inherent **two-time-axis structure**:

- **Valid time (gyldighedstid)** — when a fact was true in the real world (e.g., when a registration became effective, when an exclusion took effect)
- **Transaction time (registreringstid)** — when SKAT's system recorded the fact

Six distinct legal rules mandate that these two axes be independently tracked:

| Legal Rule | DA16 Reference | Valid ≠ Transaction time? |
|---|---|---|
| Registration effective from first delivery, notified within 10 days | DA16.3.5.1, ML § 66b stk. 3 | ✅ valid_from may precede submission by up to 10 days |
| Forced exclusion backdated to date of business move | DA16.3.5.6, momsforordningen art. 58 stk. 2 | ✅ valid_to = move date; decision date may be weeks later |
| 3-year correction window — filed in current period, applies to past period | DA16.3.6.5, ML § 66e/66n | ✅ valid time of corrected period ≠ transaction time of correction |
| 2-year re-registration ban runs from exclusion's **valid time** | DA16.3.5.6 | ✅ ban calculation uses valid_to, not decision date |
| 10-year audit trail — competent authority can request "state as of date X" | DA16.3.8 | ✅ requires point-in-time reconstruction |
| 10-day notification compliance verification | DA16.3.5.1, ML § 66b stk. 3 | ✅ requires both valid_from and notification_submitted_at |

Without bitemporality, the system cannot correctly:
- Implement backdated registrations or exclusions
- Enforce the 2-year re-registration ban when exclusion is backdated
- Produce a compliant 10-year audit trail
- Verify the 10-day notification window

---

## Decision

Implement bitemporality using a **split-responsibility pattern**:

### 1. Valid time — explicit domain columns

`scheme_registration` carries explicit valid-time columns:

```sql
valid_from                DATE NOT NULL       -- when effective in the real world
valid_to                  DATE                -- NULL = still active
notification_submitted_at TIMESTAMPTZ         -- when registrant notified SKAT
change_reason             TEXT                -- explains any retroactive valid_from/valid_to
```

**Standard query patterns:**

```sql
-- Current active registrations (as of today, recorded as of now)
WHERE valid_to IS NULL OR valid_to > CURRENT_DATE

-- As-of query: was this registration valid on a specific real-world date?
WHERE valid_from <= :date AND (valid_to IS NULL OR valid_to > :date)

-- 10-day compliance check (ML § 66b stk. 3)
WHERE notification_submitted_at::date - valid_from <= 10

-- Re-registration eligibility (2-year ban from valid_to of exclusion)
WHERE NOT EXISTS (
    SELECT 1 FROM exclusion_ban
    WHERE registrant_id = :id AND scheme_type = :scheme AND ban_lifted_at > CURRENT_DATE
)
```

### 2. Transaction time — Hibernate Envers `_AUD` tables

Transaction time (when SKAT's system recorded or changed a fact) is captured automatically by **Hibernate Envers** (ADR-0013). Every INSERT, UPDATE, and DELETE on `scheme_registration` and `registrant` produces a revision in `scheme_registration_AUD` + `REVINFO`.

**Point-in-time reconstruction (transaction time axis):**

```sql
-- State of scheme_registration as recorded by the system at a specific timestamp
SELECT sr.*
FROM scheme_registration_AUD sr
JOIN REVINFO ri ON sr.rev = ri.rev
WHERE sr.id = :id
  AND ri.rev_timestamp <= :asOfTimestamp
ORDER BY ri.rev DESC
LIMIT 1;
```

The combination of both axes supports full bitemporal queries:
> "What was the registered state of company X on 2024-03-05 *as SKAT knew it on that date* (not retroactively)?"

```sql
SELECT sr.*
FROM scheme_registration_AUD sr
JOIN REVINFO ri ON sr.rev = ri.rev
WHERE sr.registrant_id = :id
  AND ri.rev_timestamp <= :systemDate    -- transaction time axis
  AND sr.valid_from <= :realWorldDate    -- valid time axis
  AND (sr.valid_to IS NULL OR sr.valid_to > :realWorldDate)
ORDER BY ri.rev DESC
LIMIT 1;
```

### 3. Exclusion ban table

A dedicated `exclusion_ban` table stores the computed `ban_lifted_at` date (exclusion `valid_to` + 2 years), making re-registration eligibility queries fast without joining through the audit history.

---

## Consequences

**Positive:**
- Legally correct implementation of all six timing rules
- Point-in-time queries answerable on both axes independently or jointly
- Minimal schema overhead: only 4 columns added to `scheme_registration` beyond what was already needed
- Envers already required by ADR-0013 — no additional infrastructure for transaction-time tracking
- `exclusion_ban` makes 2-year ban enforcement a single indexed lookup

**Negative / constraints:**
- Queries must consistently use `valid_from`/`valid_to` (not `created_at`) for business-time filtering — developer discipline required; ArchUnit rule recommended
- Envers history tables grow indefinitely — covered by ADR-0013 retention note
- Application code must never UPDATE `valid_from` or `valid_to` directly when backdating; it must insert a new `scheme_registration` row with `valid_to` set on the superseded row, so Envers captures the change correctly
- `valid_to` on forced exclusion with backdated effective date must be set via a dedicated `ExclusionService` method, not via generic update, to enforce `change_reason` being supplied

## Scope

Bitemporality applies to:
- `registration-service`: `scheme_registration`, `registrant` (identity changes via Envers only), `exclusion_ban`
- `return-service`: `vat_return` already models valid time via `period_start`/`period_end` and transaction time via `submitted_at`; no schema changes required
- `records-service`: `vat_record` is append-only (immutable); `retain_until` is a valid-time field; no bitemporality needed

## References

- DA16.3.5.1 — Registration effective dates
- DA16.3.5.6 — Forced exclusion and re-registration ban
- DA16.3.5.7 — Voluntary deregistration
- DA16.3.6.5 — 3-year correction window
- DA16.3.8 — 10-year record retention and competent authority provision
- ML §§ 66b stk. 3, 66e, 66n, 66u
- Momsforordningen art. 58 stk. 2 (backdated exclusion on business move)
- ADR-0013 — Hibernate Envers audit trail (transaction time)
