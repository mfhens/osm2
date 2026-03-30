-- osm2-payment-service: Initial schema
-- No PII — taxable persons referenced via registrant_id UUID only.
-- Double-entry bookkeeping entries are also written to immudb for tamper-evidence.
-- Managed by Flyway — do not edit manually.

CREATE SCHEMA IF NOT EXISTS payment;

SET search_path TO payment;

CREATE TYPE payment_status AS ENUM (
    'PENDING', 'PAID', 'OVERDUE', 'REFUNDED', 'CANCELLED'
);

-- ECB exchange rates: rate_date = last day of the reporting period (ML § 66e, 66n)
CREATE TABLE ecb_exchange_rate (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    from_currency   CHAR(3)     NOT NULL,
    to_currency     CHAR(3)     NOT NULL DEFAULT 'EUR',
    rate            NUMERIC(20,10) NOT NULL,
    rate_date       DATE        NOT NULL,
    UNIQUE (from_currency, to_currency, rate_date)
);

CREATE INDEX idx_ecb_rate_lookup ON ecb_exchange_rate(from_currency, rate_date);

-- Payment record (one per return)
CREATE TABLE payment (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    registrant_id       UUID            NOT NULL,
    return_id           UUID            NOT NULL,
    amount_eur_cents    BIGINT          NOT NULL,
    local_currency      CHAR(3),
    amount_local_cents  BIGINT,
    ecb_rate_id         UUID            REFERENCES ecb_exchange_rate(id),
    status              payment_status  NOT NULL DEFAULT 'PENDING',
    due_date            DATE            NOT NULL,
    paid_at             TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_payment_registrant ON payment(registrant_id);
CREATE INDEX idx_payment_return     ON payment(return_id);

CREATE TABLE refund (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id          UUID        NOT NULL REFERENCES payment(id),
    amount_eur_cents    BIGINT      NOT NULL,
    reason              TEXT,
    refunded_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);
