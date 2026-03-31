-- osm2-scheme-service: Complete current schema (squashed from V1+V2)
-- Schema owner: osm2-scheme-service
-- Development project — squashed migration, not for production use.
-- Managed by Flyway.

CREATE SCHEMA IF NOT EXISTS scheme;

SET search_path TO scheme;

-- Reference data: the three OSS special arrangements
CREATE TABLE scheme_type (
    code            VARCHAR(10) PRIMARY KEY,
    name_da         VARCHAR(100)  NOT NULL,
    legal_basis     VARCHAR(200)  NOT NULL,
    return_cadence  VARCHAR(10)   NOT NULL CHECK (return_cadence IN ('QUARTERLY','MONTHLY')),
    active          BOOLEAN       NOT NULL DEFAULT TRUE
);

INSERT INTO scheme_type (code, name_da, legal_basis, return_cadence) VALUES
    ('NON_EU', 'Ikke-EU-ordning', 'ML §§ 66a-66f',  'QUARTERLY'),
    ('EU',     'EU-ordning',      'ML §§ 66g-66p',  'QUARTERLY'),
    ('IMPORT', 'Importordning',   'ML §§ 66q-66u',  'MONTHLY');

-- Machine-checkable eligibility rules (populated per OSS-01 spec)
CREATE TABLE eligibility_rule (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    scheme_code     VARCHAR(10)   NOT NULL REFERENCES scheme_type(code),
    rule_code       VARCHAR(50)   NOT NULL,
    description_da  TEXT          NOT NULL,
    UNIQUE (scheme_code, rule_code)
);

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

