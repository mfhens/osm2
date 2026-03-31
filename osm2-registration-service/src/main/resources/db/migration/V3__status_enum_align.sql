-- V3: Status enum alignment and missing column additions
--
-- Aligns the registrant_status PostgreSQL enum with the Java RegistrantStatus enum,
-- and adds missing columns required by the JPA entity definitions.
--
-- PENDING_VAT_NUMBER: replaces PENDING (OQ-2 assumption per OSS-02 spec)
-- CESSATION_NOTIFIED: new status for cessation notification workflow (ML § 66h)
--
-- Legal basis: ML §§ 66a–66j; Momsbekendtgørelsen §§ 115–119
-- Petition: OSS-02

SET search_path TO registration;

-- ── 1. Add new enum values ─────────────────────────────────────────────────
-- Must be done before any DML that uses the new values.
ALTER TYPE registrant_status ADD VALUE IF NOT EXISTS 'PENDING_VAT_NUMBER';
ALTER TYPE registrant_status ADD VALUE IF NOT EXISTS 'CESSATION_NOTIFIED';

-- ── 2. Make vat_number nullable on registrant ──────────────────────────────
-- Before approval the OSS VAT number is not yet assigned (PENDING_VAT_NUMBER state).
ALTER TABLE registrant ALTER COLUMN vat_number DROP NOT NULL;

-- ── 3. Align status defaults and values ───────────────────────────────────
UPDATE registrant SET status = 'PENDING_VAT_NUMBER' WHERE status = 'PENDING';
ALTER TABLE registrant ALTER COLUMN status SET DEFAULT 'PENDING_VAT_NUMBER';

-- ── 4. Add missing columns to registrant ──────────────────────────────────
ALTER TABLE registrant
    ADD COLUMN IF NOT EXISTS home_country_tax_number VARCHAR(100),
    ADD COLUMN IF NOT EXISTS bank_details             TEXT,
    ADD COLUMN IF NOT EXISTS identification_member_state CHAR(2) NOT NULL DEFAULT 'DK';

-- ── 5. Add missing columns to scheme_registration ─────────────────────────
ALTER TABLE scheme_registration
    ADD COLUMN IF NOT EXISTS vat_number                VARCHAR(50),
    ADD COLUMN IF NOT EXISTS identification_member_state CHAR(2),
    ADD COLUMN IF NOT EXISTS early_delivery_exception  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS first_delivery_date       DATE,
    ADD COLUMN IF NOT EXISTS registration_status       VARCHAR(50) NOT NULL DEFAULT 'PENDING_VAT_NUMBER',
    ADD COLUMN IF NOT EXISTS binding_period_end        DATE,
    ADD COLUMN IF NOT EXISTS binding_rule_type         VARCHAR(20),
    ADD COLUMN IF NOT EXISTS change_notification_timely BOOLEAN,
    ADD COLUMN IF NOT EXISTS delay_notification_sent   BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS expected_assignment_date  DATE,
    ADD COLUMN IF NOT EXISTS vat_number_flag           VARCHAR(20),
    ADD COLUMN IF NOT EXISTS outgoing_ims_notification_dispatched BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS closed_date               DATE,
    ADD COLUMN IF NOT EXISTS deregistration_timely     BOOLEAN,
    ADD COLUMN IF NOT EXISTS deregistration_effective_date DATE,
    ADD COLUMN IF NOT EXISTS re_registration_block_until DATE,
    ADD COLUMN IF NOT EXISTS exclusion_criterion       VARCHAR(100),
    ADD COLUMN IF NOT EXISTS exclusion_decision_date   DATE,
    ADD COLUMN IF NOT EXISTS exclusion_effective_date  DATE;

-- ── 6. Add missing columns to exclusion_ban ───────────────────────────────
-- AuditableEntity requires updated_at, updated_by, version on all entities.
ALTER TABLE exclusion_ban
    ADD COLUMN IF NOT EXISTS criterion     VARCHAR(100),
    ADD COLUMN IF NOT EXISTS updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_by    VARCHAR(255) NOT NULL DEFAULT 'system',
    ADD COLUMN IF NOT EXISTS version       BIGINT       NOT NULL DEFAULT 0;

-- ── 7. Index for new columns ───────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_scheme_reg_status
    ON scheme_registration(registration_status);

CREATE INDEX IF NOT EXISTS idx_scheme_reg_identif_ms
    ON scheme_registration(identification_member_state);
