-- osm2-records-service: Initial schema
-- Schema owner: osm2-records-service
-- Managed by Flyway — do not edit manually.
-- [TODO] Add DDL for record retention tables.

CREATE SCHEMA IF NOT EXISTS records;

SET search_path TO records;

-- Placeholder: vat_record, record_access_log tables per OSS-06.
-- Records must be retained for 10 years. Dual-access: IMS + CMS.
-- Note: No PII — taxable persons referenced via registrant_id UUID only.
