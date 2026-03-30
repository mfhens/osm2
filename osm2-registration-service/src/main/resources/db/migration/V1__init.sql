-- osm2-registration-service: Initial schema
-- PII SILO — taxable person identity data lives exclusively in this service.
-- All other services reference taxable persons via registrant_id UUID only.
--
-- BITEMPORALITY (ADR-0033):
--   Valid time  (gyldighedstid)    — when a registration/exclusion was true in the real world.
--   Transaction time (registreringstid) — when SKAT recorded it, tracked via Hibernate Envers _AUD tables.
--   The combination of explicit valid_from/valid_to columns (below) and Envers revision history
--   satisfies ML §§ 66b, 66e, 66n and momsforordningen art. 58 stk. 2.
--
-- Managed by Flyway — do not edit manually.

CREATE SCHEMA IF NOT EXISTS registration;

SET search_path TO registration;

CREATE TYPE registrant_status AS ENUM (
    'PENDING', 'ACTIVE', 'SUSPENDED', 'EXCLUDED', 'DEREGISTERED'
);
CREATE TYPE scheme_type AS ENUM ('NON_EU', 'EU', 'IMPORT');

-- Core PII table — one row per registered taxable person or intermediary.
-- Identity changes (name, address, VAT number) are tracked via Envers _AUD (transaction time).
CREATE TABLE registrant (
    id              UUID                PRIMARY KEY DEFAULT gen_random_uuid(),
    scheme_type     scheme_type         NOT NULL,
    status          registrant_status   NOT NULL DEFAULT 'PENDING',
    -- Identity (PII)
    vat_number      VARCHAR(50)         NOT NULL,
    country_code    CHAR(2)             NOT NULL,
    legal_name      VARCHAR(200)        NOT NULL,
    address_line1   VARCHAR(200),
    address_line2   VARCHAR(200),
    city            VARCHAR(100),
    postal_code     VARCHAR(20),
    contact_email   VARCHAR(200),
    contact_phone   VARCHAR(50),
    -- EU scheme binding: ML § 66k — current year + 2 following calendar years
    binding_start   DATE,
    binding_end     DATE,
    -- Audit
    created_at      TIMESTAMPTZ         NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ         NOT NULL DEFAULT now(),
    version         BIGINT              NOT NULL DEFAULT 0
);

CREATE INDEX idx_registrant_vat    ON registrant(vat_number, country_code);
CREATE INDEX idx_registrant_status ON registrant(status);

-- Bitemporal registration lifecycle events.
--
-- VALID TIME columns (gyldighedstid):
--   valid_from   — when the registration became effective in the real world.
--                  May differ from notification_submitted_at (retroactive first-delivery rule, ML § 66b stk. 3).
--   valid_to     — when the registration ceased to be effective (NULL = still active).
--                  For forced exclusion due to business move: set to the date of the move,
--                  which can be weeks before the decision (momsforordningen art. 58 stk. 2).
--
-- TRANSACTION TIME is NOT stored as explicit columns here; it is tracked automatically
-- by Hibernate Envers _AUD tables (revision number + REVINFO.rev_timestamp).
-- To query "what was valid as of system date T": JOIN with REVINFO WHERE rev_timestamp <= T.
--
-- notification_submitted_at — the moment the taxable person submitted their notification to SKAT.
--   Required for the 10-day compliance check (valid_from - notification_submitted_at <= 10 days).
--
-- change_reason — free-text explanation for any retroactive valid_from/valid_to value,
--   e.g., "Retroactive per first delivery rule (ML § 66b stk. 3)" or
--         "Backdated exclusion — business move on [date] per momsforordningen art. 58 stk. 2".
CREATE TABLE scheme_registration (
    id                          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    registrant_id               UUID        NOT NULL REFERENCES registrant(id),
    scheme_type                 scheme_type NOT NULL,
    -- Valid time (gyldighedstid)
    valid_from                  DATE        NOT NULL,
    valid_to                    DATE,
    -- Notification timing (for 10-day compliance check, ML § 66b stk. 3)
    notification_submitted_at   TIMESTAMPTZ,
    -- Context for any retroactive valid_from or valid_to
    change_reason               TEXT,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_scheme_reg_registrant  ON scheme_registration(registrant_id);
CREATE INDEX idx_scheme_reg_valid_range ON scheme_registration(valid_from, valid_to);

-- Re-registration ban tracking (ML § 66d stk. 3 / § 66m stk. 3 / § 66v stk. 3).
-- 2 years from the *valid_from* of the exclusion event (not the decision date).
-- Queried before allowing a new scheme_registration for the same registrant.
CREATE TABLE exclusion_ban (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    registrant_id   UUID        NOT NULL REFERENCES registrant(id),
    scheme_type     scheme_type NOT NULL,
    -- The exclusion event that triggered the ban
    exclusion_reg_id UUID       NOT NULL REFERENCES scheme_registration(id),
    -- Ban is lifted at: exclusion valid_to + 2 years (indexed to valid time, not decision date)
    ban_lifted_at   DATE        NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_exclusion_ban_registrant ON exclusion_ban(registrant_id, scheme_type, ban_lifted_at);

-- Import scheme: the intermediary entity (joint liability, ML § 66s)
CREATE TABLE intermediary (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    registrant_id       UUID        NOT NULL REFERENCES registrant(id),
    intermediary_vat    VARCHAR(50) NOT NULL,
    country_code        CHAR(2)     NOT NULL
);

-- Principals represented by an intermediary
CREATE TABLE principal (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    intermediary_id UUID        NOT NULL REFERENCES intermediary(id),
    principal_vat   VARCHAR(50) NOT NULL,
    country_code    CHAR(2)     NOT NULL,
    legal_name      VARCHAR(200) NOT NULL,
    active          BOOLEAN     NOT NULL DEFAULT TRUE
);
