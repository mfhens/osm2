-- V3 used DROP CONSTRAINT IF EXISTS with a 64-character name; PostgreSQL truncates
-- identifiers to 63 chars, so the real constraint name differs and the drop was a no-op.
-- The old UNIQUE on (registrant_id, scheme_type, period_start, period_end) then blocked
-- demo seed correction rows. Drop any UNIQUE constraint on vat_return via pg_catalog.

SET search_path TO return_svc;

DO $$
DECLARE
  con RECORD;
BEGIN
  FOR con IN
    SELECT c.conname
    FROM pg_constraint c
    JOIN pg_class t ON c.conrelid = t.oid
    JOIN pg_namespace n ON t.relnamespace = n.oid
    WHERE n.nspname = 'return_svc'
      AND t.relname = 'vat_return'
      AND c.contype = 'u'
  LOOP
    EXECUTE format('ALTER TABLE return_svc.vat_return DROP CONSTRAINT %I', con.conname);
  END LOOP;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_vat_return_active_period
    ON return_svc.vat_return (registrant_id, scheme_type, period_start, period_end)
    WHERE status <> 'CORRECTED'::return_status;
