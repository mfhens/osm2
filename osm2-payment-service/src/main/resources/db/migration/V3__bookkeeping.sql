-- V3: Bookkeeping — double-entry ledger (ufst-bookkeeping-core integration [ADR-0018])
-- LedgerEntry and FinancialEvent tables persist the core bookkeeping domain.
-- immudb is used as a secondary tamper-evident store (dual-write in LedgerEntryJpaAdapter).
-- No PII — taxable persons referenced via debt_id (UUID) only.
-- Managed by Flyway — do not edit manually.

SET search_path TO payment;

-- One row per side (debit or credit) of a balanced double-entry transaction pair.
-- Each double-entry transaction produces exactly two rows sharing the same transaction_id.
CREATE TABLE ledger_entry (
    id                          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id              UUID            NOT NULL,   -- shared by debit + credit pair
    debt_id                     UUID            NOT NULL,
    account_code                VARCHAR(20)     NOT NULL,
    account_name                VARCHAR(100)    NOT NULL,
    entry_type                  VARCHAR(10)     NOT NULL,   -- DEBIT | CREDIT
    amount                      NUMERIC(19,4)   NOT NULL,
    effective_date              DATE            NOT NULL,
    posting_date                DATE            NOT NULL,
    reference                   VARCHAR(255),
    description                 VARCHAR(500),
    reversal_of_transaction_id  UUID,                       -- non-null for storno entries
    entry_category              VARCHAR(30)     NOT NULL,   -- see EntryCategory enum

    -- Audit columns (AuditableEntity / audit-trail-commons)
    created_at                  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    created_by                  VARCHAR(100)    NOT NULL DEFAULT 'system',
    updated_by                  VARCHAR(100)    NOT NULL DEFAULT 'system',
    version                     BIGINT          NOT NULL DEFAULT 0
);

CREATE INDEX idx_ledger_entry_debt_id
    ON ledger_entry(debt_id);

CREATE INDEX idx_ledger_entry_transaction_id
    ON ledger_entry(transaction_id);

-- Partial index: only storno entries carry a reversal reference
CREATE INDEX idx_ledger_entry_reversal_of_txn
    ON ledger_entry(reversal_of_transaction_id)
    WHERE reversal_of_transaction_id IS NOT NULL;

-- Partial index: accelerates findInterestAccrualsAfterDate
CREATE INDEX idx_ledger_entry_interest_accrual
    ON ledger_entry(debt_id, effective_date)
    WHERE entry_category = 'INTEREST_ACCRUAL';

-- Financial event timeline: one event per domain occurrence in the debt lifecycle.
-- Used by InterestAccrualService and TimelineReplayService to replay principal changes.
CREATE TABLE financial_event (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    debt_id                 UUID            NOT NULL,
    event_type              VARCHAR(30)     NOT NULL,   -- see EventType enum
    effective_date          DATE            NOT NULL,
    amount                  NUMERIC(19,4)   NOT NULL,
    corrects_event_id       UUID,                       -- non-null for CORRECTION events
    reference               VARCHAR(255),
    description             VARCHAR(500),
    ledger_transaction_id   UUID,                       -- links event to its ledger posting

    -- Audit columns (AuditableEntity / audit-trail-commons)
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT now(),
    created_by              VARCHAR(100)    NOT NULL DEFAULT 'system',
    updated_by              VARCHAR(100)    NOT NULL DEFAULT 'system',
    version                 BIGINT          NOT NULL DEFAULT 0
);

CREATE INDEX idx_financial_event_debt_id
    ON financial_event(debt_id);

-- Composite index: covers the ORDER BY used by findByDebtIdOrderByEffectiveDateAscCreatedAtAsc
CREATE INDEX idx_financial_event_debt_effective_created
    ON financial_event(debt_id, effective_date, created_at);
