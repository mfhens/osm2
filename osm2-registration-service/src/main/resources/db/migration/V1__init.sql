-- osm2-registration-service: Complete current schema (squashed from V1+V2+V3+V4)
-- PII SILO — taxable person identity data lives exclusively in this service.
-- All other services reference taxable persons via registrant_id UUID only.
--
-- BITEMPORALITY (ADR-0033):
--   Valid time  (gyldighedstid)    — when a registration/exclusion was true in the real world.
--   Transaction time (registreringstid) — when SKAT recorded it, tracked via Hibernate Envers _AUD tables.
--   The combination of explicit valid_from/valid_to columns (below) and Envers revision history
--   satisfies ML §§ 66b, 66e, 66n and momsforordningen art. 58 stk. 2.
--
-- Development project — squashed migration, not for production use.
-- Managed by Flyway.

CREATE SCHEMA IF NOT EXISTS registration;

SET search_path TO registration;

-- Audit context function for audit-trail-commons integration
CREATE OR REPLACE FUNCTION public.set_audit_context(
    p_user_id     TEXT,
    p_client_ip   INET,
    p_application TEXT
) RETURNS VOID AS $$
BEGIN
    PERFORM set_config('audit.user_id',     p_user_id,                                    true);
    PERFORM set_config('audit.client_ip',   COALESCE(p_client_ip::TEXT, 'unknown'),       true);
    PERFORM set_config('audit.application', p_application,                                true);
END;
$$ LANGUAGE plpgsql;

CREATE TYPE registrant_status AS ENUM (
    'PENDING_VAT_NUMBER', 'ACTIVE', 'SUSPENDED', 'EXCLUDED', 'DEREGISTERED', 'CESSATION_NOTIFIED'
);
CREATE TYPE scheme_type AS ENUM ('NON_EU', 'EU', 'IMPORT');

-- Core PII table — one row per registered taxable person or intermediary.
-- Identity changes (name, address, VAT number) are tracked via Envers _AUD (transaction time).
CREATE TABLE registrant (
    id                          UUID                PRIMARY KEY DEFAULT gen_random_uuid(),
    scheme_type                 scheme_type         NOT NULL,
    status                      registrant_status   NOT NULL DEFAULT 'PENDING_VAT_NUMBER',
    -- Identity (PII)
    vat_number                  VARCHAR(50),
    country_code                VARCHAR(2)             NOT NULL,
    legal_name                  VARCHAR(200)        NOT NULL,
    address_line1               VARCHAR(200),
    address_line2               VARCHAR(200),
    city                        VARCHAR(100),
    postal_code                 VARCHAR(20),
    contact_email               VARCHAR(200),
    contact_phone               VARCHAR(50),
    -- Additional identity fields
    home_country_tax_number     VARCHAR(100),
    bank_details                TEXT,
    identification_member_state VARCHAR(2)             NOT NULL DEFAULT 'DK',
    -- EU scheme binding: ML § 66k — current year + 2 following calendar years
    binding_start               DATE,
    binding_end                 DATE,
    -- Audit
    created_at                  TIMESTAMPTZ         NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ         NOT NULL DEFAULT now(),
    created_by                  VARCHAR(255)        NOT NULL DEFAULT 'system',
    updated_by                  VARCHAR(255)        NOT NULL DEFAULT 'system',
    version                     BIGINT              NOT NULL DEFAULT 0
);

CREATE INDEX idx_registrant_vat    ON registrant(vat_number, country_code);
CREATE INDEX idx_registrant_status ON registrant(status);

-- Bitemporal registration lifecycle events.
--
-- VALID TIME columns (gyldighedstid):
--   valid_from   — when the registration became effective in the real world.
--   valid_to     — when the registration ceased to be effective (NULL = still active).
-- TRANSACTION TIME is tracked by Hibernate Envers _AUD tables.
CREATE TABLE scheme_registration (
    id                                      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    registrant_id                           UUID        NOT NULL REFERENCES registrant(id),
    scheme_type                             scheme_type NOT NULL,
    -- Valid time (gyldighedstid)
    valid_from                              DATE        NOT NULL,
    valid_to                                DATE,
    -- Notification timing (for 10-day compliance check, ML § 66b stk. 3)
    notification_submitted_at               TIMESTAMPTZ,
    -- Context for any retroactive valid_from or valid_to
    change_reason                           TEXT,
    -- Registration lifecycle fields (from V3)
    vat_number                              VARCHAR(50),
    identification_member_state             VARCHAR(2),
    early_delivery_exception                BOOLEAN     NOT NULL DEFAULT FALSE,
    first_delivery_date                     DATE,
    registration_status                     VARCHAR(50) NOT NULL DEFAULT 'PENDING_VAT_NUMBER',
    binding_period_end                      DATE,
    binding_rule_type                       VARCHAR(20),
    change_notification_timely              BOOLEAN,
    delay_notification_sent                 BOOLEAN     NOT NULL DEFAULT FALSE,
    expected_assignment_date                DATE,
    vat_number_flag                         VARCHAR(20),
    outgoing_ims_notification_dispatched    BOOLEAN     NOT NULL DEFAULT FALSE,
    closed_date                             DATE,
    deregistration_timely                   BOOLEAN,
    deregistration_effective_date           DATE,
    re_registration_block_until             DATE,
    exclusion_criterion                     VARCHAR(100),
    exclusion_decision_date                 DATE,
    exclusion_effective_date                DATE,
    -- Scheme-switch and transitional fields (from V4)
    new_scheme_effective_date               DATE,
    transitional_update_overdue             BOOLEAN     NOT NULL DEFAULT FALSE,
    last_identification_update_date         DATE,
    -- Audit
    created_at                              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                              TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by                              VARCHAR(255) NOT NULL DEFAULT 'system',
    updated_by                              VARCHAR(255) NOT NULL DEFAULT 'system',
    version                                 BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_scheme_reg_registrant   ON scheme_registration(registrant_id);
CREATE INDEX idx_scheme_reg_valid_range  ON scheme_registration(valid_from, valid_to);
CREATE INDEX idx_scheme_reg_status       ON scheme_registration(registration_status);
CREATE INDEX idx_scheme_reg_identif_ms   ON scheme_registration(identification_member_state);

-- Re-registration ban tracking (ML § 66d stk. 3 / § 66m stk. 3 / § 66v stk. 3).
CREATE TABLE exclusion_ban (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    registrant_id   UUID        NOT NULL REFERENCES registrant(id),
    scheme_type     scheme_type NOT NULL,
    exclusion_reg_id UUID       NOT NULL REFERENCES scheme_registration(id),
    ban_lifted_at   DATE        NOT NULL,
    criterion       VARCHAR(100),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      VARCHAR(255) NOT NULL DEFAULT 'system',
    updated_by      VARCHAR(255) NOT NULL DEFAULT 'system',
    version         BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_exclusion_ban_registrant ON exclusion_ban(registrant_id, scheme_type, ban_lifted_at);


-- ============================================================================
-- HIBERNATE ENVERS AUDIT TABLES (transaction time / registreringstid)
-- Fulfils ML §§ 66b, 66e, 66n and momsforordningen art. 58 stk. 2.
-- revinfo: standard Envers revision metadata.
-- registrant_aud / scheme_registration_aud: per-revision snapshots of each
-- audited entity. All non-PK columns are nullable per Envers convention.
-- ============================================================================

CREATE SEQUENCE IF NOT EXISTS revinfo_seq START 1 INCREMENT BY 50;

CREATE TABLE revinfo (
    rev      INTEGER  PRIMARY KEY DEFAULT nextval('revinfo_seq'),
    revtstmp BIGINT   NOT NULL
);

CREATE TABLE registrant_aud (
    id                          UUID        NOT NULL,
    rev                         INTEGER     NOT NULL REFERENCES revinfo(rev),
    revtype                     SMALLINT,
    created_at                  TIMESTAMP,
    updated_at                  TIMESTAMP,
    created_by                  VARCHAR(255),
    updated_by                  VARCHAR(255),
    version                     BIGINT,
    legal_name                  VARCHAR(200),
    country_code                VARCHAR(2),
    home_country_tax_number     VARCHAR(100),
    address_line1               VARCHAR(200),
    contact_email               VARCHAR(200),
    contact_phone               VARCHAR(50),
    bank_details                TEXT,
    identification_member_state VARCHAR(2),
    scheme_type                 scheme_type,
    status                      registrant_status,
    vat_number                  VARCHAR(50),
    PRIMARY KEY (id, rev)
);

CREATE TABLE scheme_registration_aud (
    id                                      UUID        NOT NULL,
    rev                                     INTEGER     NOT NULL REFERENCES revinfo(rev),
    revtype                                 SMALLINT,
    created_at                              TIMESTAMP,
    updated_at                              TIMESTAMP,
    created_by                              VARCHAR(255),
    updated_by                              VARCHAR(255),
    version                                 BIGINT,
    registrant_id                           UUID,
    scheme_type                             scheme_type,
    valid_from                              DATE,
    valid_to                                DATE,
    notification_submitted_at               TIMESTAMP,
    vat_number                              VARCHAR(50),
    change_reason                           TEXT,
    identification_member_state             VARCHAR(2),
    early_delivery_exception                BOOLEAN,
    first_delivery_date                     DATE,
    registration_status                     VARCHAR(50),
    binding_period_end                      DATE,
    binding_rule_type                       VARCHAR(20),
    change_notification_timely              BOOLEAN,
    delay_notification_sent                 BOOLEAN,
    expected_assignment_date                DATE,
    vat_number_flag                         VARCHAR(20),
    outgoing_ims_notification_dispatched    BOOLEAN,
    closed_date                             DATE,
    deregistration_timely                   BOOLEAN,
    deregistration_effective_date           DATE,
    re_registration_block_until             DATE,
    exclusion_criterion                     VARCHAR(100),
    exclusion_decision_date                 DATE,
    exclusion_effective_date                DATE,
    new_scheme_effective_date               DATE,
    transitional_update_overdue             BOOLEAN,
    last_identification_update_date         DATE,
    PRIMARY KEY (id, rev)
);


-- Import scheme: the intermediary entity (joint liability, ML § 66s)
CREATE TABLE intermediary (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    registrant_id       UUID        NOT NULL REFERENCES registrant(id),
    intermediary_vat    VARCHAR(50) NOT NULL,
    country_code        VARCHAR(2)     NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by          VARCHAR(255) NOT NULL DEFAULT 'system',
    updated_by          VARCHAR(255) NOT NULL DEFAULT 'system',
    version             BIGINT       NOT NULL DEFAULT 0
);

-- Principals represented by an intermediary
CREATE TABLE principal (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    intermediary_id UUID        NOT NULL REFERENCES intermediary(id),
    principal_vat   VARCHAR(50) NOT NULL,
    country_code    VARCHAR(2)     NOT NULL,
    legal_name      VARCHAR(200) NOT NULL,
    active          BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by      VARCHAR(255) NOT NULL DEFAULT 'system',
    updated_by      VARCHAR(255) NOT NULL DEFAULT 'system',
    version         BIGINT       NOT NULL DEFAULT 0
);


