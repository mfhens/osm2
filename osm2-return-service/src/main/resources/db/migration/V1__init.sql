-- osm2-return-service: Initial schema
-- No PII — taxable persons referenced via registrant_id UUID only.
-- Managed by Flyway — do not edit manually.

CREATE SCHEMA IF NOT EXISTS return_svc;

SET search_path TO return_svc;

CREATE TYPE return_status AS ENUM (
    'DRAFT', 'SUBMITTED', 'ACCEPTED', 'LATE', 'CORRECTED', 'NIL'
);
CREATE TYPE scheme_type AS ENUM ('NON_EU', 'EU', 'IMPORT');

-- One row per reporting period per registrant
CREATE TABLE vat_return (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    registrant_id       UUID            NOT NULL,
    scheme_type         scheme_type     NOT NULL,
    period_start        DATE            NOT NULL,
    period_end          DATE            NOT NULL,
    deadline            DATE            NOT NULL,
    status              return_status   NOT NULL DEFAULT 'DRAFT',
    nil_return          BOOLEAN         NOT NULL DEFAULT FALSE,
    submitted_at        TIMESTAMPTZ,
    -- Corrections: new return references the one it corrects (ML § 66e, 66n, post-Jul 2021)
    corrects_return_id  UUID            REFERENCES vat_return(id),
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    version             BIGINT          NOT NULL DEFAULT 0,
    UNIQUE (registrant_id, scheme_type, period_start, period_end)
);

CREATE INDEX idx_vat_return_registrant ON vat_return(registrant_id);
CREATE INDEX idx_vat_return_status     ON vat_return(status);

-- One line per member state per return (amounts in EUR cents to avoid floating point)
CREATE TABLE return_line (
    id                      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    return_id               UUID        NOT NULL REFERENCES vat_return(id),
    member_state            CHAR(2)     NOT NULL,
    taxable_amount_eur_cents BIGINT     NOT NULL,
    vat_rate_pct            NUMERIC(5,2) NOT NULL,
    vat_amount_eur_cents    BIGINT      NOT NULL
);

CREATE INDEX idx_return_line_return ON return_line(return_id);
