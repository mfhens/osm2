-- V2: audit-trail-commons integration
--
-- scheme_type and eligibility_rule are static reference data; no AuditableEntity needed.
-- The set_audit_context() function is required for AuditContextFilter to work within
-- the scheme-service database.

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
