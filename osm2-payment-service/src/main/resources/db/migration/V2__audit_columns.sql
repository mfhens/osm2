-- V2: audit-trail-commons integration
--
-- Creates the set_audit_context() function and adds missing audit-identity
-- columns required by AuditableEntity (created_by, updated_by, version).

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

SET search_path TO payment;

-- payment: already has created_at, updated_at; add identity + optimistic lock
ALTER TABLE payment
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255) NOT NULL DEFAULT 'system',
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255) NOT NULL DEFAULT 'system',
    ADD COLUMN IF NOT EXISTS version    BIGINT       NOT NULL DEFAULT 0;

-- refund: only has refunded_at; add full audit columns
-- refund is insert-only in practice but extends AuditableEntity for consistency
ALTER TABLE refund
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255) NOT NULL DEFAULT 'system',
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255) NOT NULL DEFAULT 'system',
    ADD COLUMN IF NOT EXISTS version    BIGINT       NOT NULL DEFAULT 0;
