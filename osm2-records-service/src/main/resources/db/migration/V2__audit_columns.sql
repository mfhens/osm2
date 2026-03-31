-- V2: audit-trail-commons integration
--
-- Creates the set_audit_context() function.
-- Records-service tables are append-only; only created_by is added.
-- vat_record:          created_at already present; add created_by.
-- record_access_log:   accessor identity captured in accessor_id/accessor_type;
--                      add created_by for system-level attribution (e.g., SYSTEM actor).

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

SET search_path TO records;

ALTER TABLE vat_record
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255) NOT NULL DEFAULT 'system';

ALTER TABLE record_access_log
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255) NOT NULL DEFAULT 'system';
