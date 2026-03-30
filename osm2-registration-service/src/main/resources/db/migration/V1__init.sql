-- osm2-registration-service: Initial schema
-- Schema owner: osm2-registration-service (PII SILO — contains taxable person identity data)
-- Managed by Flyway — do not edit manually.
-- [TODO] Add DDL for registration tables including VAT numbers, addresses, contact details.

CREATE SCHEMA IF NOT EXISTS registration;

SET search_path TO registration;

-- PII silo: This schema holds taxable person identity data (VAT numbers, addresses, contact details).
-- All other services reference taxable persons via registrant_id UUID only.
-- Placeholder: registrant, scheme_registration, deregistration tables will be defined per OSS-02, OSS-03.
