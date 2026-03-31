-- V2: audit-trail-commons integration
--
-- Creates the set_audit_context() function used by AuditContextService
-- (dk.ufst.opendebt.common.audit) to stamp PostgreSQL session variables for
-- database-level audit tracking.  Function lives in the public schema so it
-- is reachable regardless of the active search_path.
--
-- Also adds the missing audit-identity columns required by AuditableEntity:
--   created_by  — who inserted the row (populated by AuditorAware<String>)
--   updated_by  — who last changed the row
--   version     — optimistic-lock counter (@Version)
--
-- Tables that are append-only (exclusion_ban) receive only created_by.
-- Tables with no prior audit columns (intermediary, principal) receive the full set.

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

SET search_path TO registration;

-- registrant: already has created_at, updated_at, version
ALTER TABLE registrant
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255) NOT NULL DEFAULT 'system',
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255) NOT NULL DEFAULT 'system';

-- scheme_registration: already has created_at, updated_at
ALTER TABLE scheme_registration
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255) NOT NULL DEFAULT 'system',
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255) NOT NULL DEFAULT 'system',
    ADD COLUMN IF NOT EXISTS version    BIGINT       NOT NULL DEFAULT 0;

-- exclusion_ban: append-only — created_by only
ALTER TABLE exclusion_ban
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255) NOT NULL DEFAULT 'system';

-- intermediary: no audit columns yet
ALTER TABLE intermediary
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255) NOT NULL DEFAULT 'system',
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255) NOT NULL DEFAULT 'system',
    ADD COLUMN IF NOT EXISTS version    BIGINT       NOT NULL DEFAULT 0;

-- principal: no audit columns yet
ALTER TABLE principal
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255) NOT NULL DEFAULT 'system',
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255) NOT NULL DEFAULT 'system',
    ADD COLUMN IF NOT EXISTS version    BIGINT       NOT NULL DEFAULT 0;
