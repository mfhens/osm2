-- osm2-return-service: Initial schema
-- Schema owner: osm2-return-service
-- Managed by Flyway — do not edit manually.
-- [TODO] Add DDL for VAT return tables.

CREATE SCHEMA IF NOT EXISTS return_svc;

SET search_path TO return_svc;

-- Placeholder: vat_return, return_correction, late_return_reminder tables per OSS-04.
-- Note: No PII — taxable persons referenced via registrant_id UUID only.
