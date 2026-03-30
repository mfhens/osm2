-- osm2-records-service: Initial schema
-- 10-year retention per ML § 66f/66o/66u. Dual-access: IMS (taxable person) + CMS (authority).
-- No PII — taxable persons referenced via registrant_id UUID only.
-- Managed by Flyway — do not edit manually.

CREATE SCHEMA IF NOT EXISTS records;

SET search_path TO records;

CREATE TYPE record_type AS ENUM (
    'REGISTRATION', 'RETURN', 'PAYMENT', 'CORRECTION', 'EXCLUSION'
);
CREATE TYPE accessor_type AS ENUM (
    'REGISTRANT', 'INTERMEDIARY', 'CASEWORKER', 'SYSTEM'
);

-- Immutable record per VAT event (append-only; no UPDATE/DELETE in application)
CREATE TABLE vat_record (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    registrant_id   UUID        NOT NULL,
    record_type     record_type NOT NULL,
    period_start    DATE,
    period_end      DATE,
    source_id       UUID,                       -- opaque cross-service reference
    content_json    JSONB       NOT NULL,
    retain_until    DATE        NOT NULL,        -- created_at::date + 10 years
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_vat_record_registrant    ON vat_record(registrant_id);
CREATE INDEX idx_vat_record_retain_until  ON vat_record(retain_until);
CREATE INDEX idx_vat_record_source        ON vat_record(source_id);

-- Every access to a vat_record is logged (ML § 66f/66o/66u dual-access requirement)
CREATE TABLE record_access_log (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    record_id       UUID            NOT NULL REFERENCES vat_record(id),
    accessor_type   accessor_type   NOT NULL,
    accessor_id     UUID            NOT NULL,
    accessed_at     TIMESTAMPTZ     NOT NULL DEFAULT now(),
    purpose         TEXT            NOT NULL
);

CREATE INDEX idx_access_log_record   ON record_access_log(record_id);
CREATE INDEX idx_access_log_accessor ON record_access_log(accessor_id);
