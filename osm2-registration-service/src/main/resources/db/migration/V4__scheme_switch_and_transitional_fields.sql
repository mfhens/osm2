-- V4: Scheme-switch and transitional-provision fields
--
-- Adds columns required by SchemeSwitchService and TransitionalComplianceService.
--
-- Legal basis: ML §§ 66d, 66j; Direktiv 2017/2455 (transitional)
-- Petition: OSS-02 — FR-OSS-02-037, FR-OSS-02-038

SET search_path TO registration;

ALTER TABLE scheme_registration
    ADD COLUMN IF NOT EXISTS new_scheme_effective_date         DATE,
    ADD COLUMN IF NOT EXISTS transitional_update_overdue       BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS last_identification_update_date   DATE;
