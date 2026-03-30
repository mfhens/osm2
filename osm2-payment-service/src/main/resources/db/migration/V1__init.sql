-- osm2-payment-service: Initial schema
-- Schema owner: osm2-payment-service
-- Managed by Flyway — do not edit manually.
-- [TODO] Add DDL for payment and bookkeeping tables.

CREATE SCHEMA IF NOT EXISTS payment;

SET search_path TO payment;

-- Placeholder: payment, payment_distribution, ecb_exchange_rate, refund tables per OSS-05.
-- Note: No PII — taxable persons referenced via registrant_id UUID only.
-- Double-entry bookkeeping ledger entries are also recorded in immudb for tamper-evidence.
