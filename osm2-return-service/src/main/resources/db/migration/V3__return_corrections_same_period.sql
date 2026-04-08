-- Allow a superseded return (CORRECTED) and its replacement (ACCEPTED) for the same
-- (registrant, scheme, period). The V1 UNIQUE on those four columns made demo seed V9001
-- skip the correction row (ON CONFLICT DO NOTHING), breaking FK on return_line.

SET search_path TO return_svc;

ALTER TABLE vat_return
    DROP CONSTRAINT IF EXISTS vat_return_registrant_id_scheme_type_period_start_period_end_key;

-- At most one "active" return per period; superseded originals stay CORRECTED and are excluded.
CREATE UNIQUE INDEX uq_vat_return_active_period
    ON vat_return (registrant_id, scheme_type, period_start, period_end)
    WHERE status <> 'CORRECTED'::return_status;
