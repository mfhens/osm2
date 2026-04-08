-- V9001__seed_demo_data.sql  (records-service)
-- Demo profile only — loaded via classpath:db/seed when SPRING_PROFILES_ACTIVE=demo.
-- Records are retained for 10 years (retain_until = created_at::date + 10 years).
-- All UUID literals use hex digits only (PostgreSQL uuid type).
-- Idempotent: INSERT ... ON CONFLICT DO NOTHING

SET search_path TO records;

-- ── Registration records ──────────────────────────────────────────────────────

INSERT INTO vat_record (id, registrant_id, record_type, period_start, period_end,
                        source_id, content_json, retain_until, created_at)
VALUES
    ('c0a00001-0000-4000-8000-000000000001',
     '11111111-1111-1111-1111-111111111111',
     'REGISTRATION', '2023-01-01', NULL,
     'd0a00001-0001-4000-8000-000000000001',
     '{"scheme":"EU","vatNumber":"DK12345678","legalName":"Nina Hansen ApS","effectiveFrom":"2023-01-01"}',
     '2033-01-01', '2023-01-01 00:00:00+00'),

    ('c0a00002-0000-4000-8000-000000000001',
     '22222222-2222-2222-2222-222222222222',
     'REGISTRATION', '2022-07-01', NULL,
     'd0a00002-0001-4000-8000-000000000001',
     '{"scheme":"NON_EU","vatNumber":"NO987654321MVA","legalName":"Nordic Konsulent AS","effectiveFrom":"2022-07-01"}',
     '2032-07-01', '2022-07-01 00:00:00+00'),

    ('c0a00003-0000-4000-8000-000000000001',
     '33333333-3333-3333-3333-333333333333',
     'REGISTRATION', '2021-07-01', NULL,
     'd0a00003-0001-4000-8000-000000000001',
     '{"scheme":"IMPORT","vatNumber":"ATU12345678","legalName":"EasyProxy GmbH","effectiveFrom":"2021-07-01","intermediary":true}',
     '2031-07-01', '2021-07-01 00:00:00+00')
ON CONFLICT DO NOTHING;

-- ── Return records: EU scheme ─────────────────────────────────────────────────

INSERT INTO vat_record (id, registrant_id, record_type, period_start, period_end,
                        source_id, content_json, retain_until, created_at)
VALUES
    ('c0b00001-0000-4000-8000-000000000001',
     '11111111-1111-1111-1111-111111111111',
     'RETURN', '2023-07-01', '2023-09-30',
     'aaaa0001-0000-0000-0000-000000000000',
     '{"returnId":"aaaa0001-0000-0000-0000-000000000000","scheme":"EU","totalVatEurCents":204000,"status":"ACCEPTED"}',
     '2033-10-20', '2023-10-20 09:15:00+00'),

    ('c0b00002-0000-4000-8000-000000000001',
     '11111111-1111-1111-1111-111111111111',
     'RETURN', '2023-10-01', '2023-12-31',
     'aaaa0002-0000-0000-0000-000000000000',
     '{"returnId":"aaaa0002-0000-0000-0000-000000000000","scheme":"EU","totalVatEurCents":170300,"status":"ACCEPTED"}',
     '2034-01-28', '2024-01-28 14:03:00+00'),

    ('c0b00003-0000-4000-8000-000000000001',
     '11111111-1111-1111-1111-111111111111',
     'CORRECTION', '2024-01-01', '2024-03-31',
     'aaaa0004-0000-0000-0000-000000000000',
     '{"returnId":"aaaa0004-0000-0000-0000-000000000000","correctsReturnId":"aaaa0003-0000-0000-0000-000000000000","scheme":"EU","totalVatEurCents":100700,"status":"ACCEPTED"}',
     '2034-06-10', '2024-06-10 10:30:00+00')
ON CONFLICT DO NOTHING;

-- ── Return records: Non-EU scheme ─────────────────────────────────────────────

INSERT INTO vat_record (id, registrant_id, record_type, period_start, period_end,
                        source_id, content_json, retain_until, created_at)
VALUES
    ('c0c00001-0000-4000-8000-000000000001',
     '22222222-2222-2222-2222-222222222222',
     'RETURN', '2023-07-01', '2023-09-30',
     'bbbb0001-0000-0000-0000-000000000000',
     '{"returnId":"bbbb0001-0000-0000-0000-000000000000","scheme":"NON_EU","totalVatEurCents":136000,"status":"ACCEPTED"}',
     '2033-10-29', '2023-10-29 16:45:00+00'),

    ('c0c00002-0000-4000-8000-000000000001',
     '22222222-2222-2222-2222-222222222222',
     'RETURN', '2023-10-01', '2023-12-31',
     'bbbb0002-0000-0000-0000-000000000000',
     '{"returnId":"bbbb0002-0000-0000-0000-000000000000","scheme":"NON_EU","nilReturn":true,"status":"NIL"}',
     '2034-01-15', '2024-01-15 08:00:00+00')
ON CONFLICT DO NOTHING;

-- ── Payment records ───────────────────────────────────────────────────────────

INSERT INTO vat_record (id, registrant_id, record_type, period_start, period_end,
                        source_id, content_json, retain_until, created_at)
VALUES
    ('c0d00001-0000-4000-8000-000000000001',
     '11111111-1111-1111-1111-111111111111',
     'PAYMENT', '2023-07-01', '2023-09-30',
     'dddd0001-0000-0000-0000-000000000000',
     '{"paymentId":"dddd0001-0000-0000-0000-000000000000","amountEurCents":204000,"status":"PAID","paidAt":"2023-10-25"}',
     '2033-10-25', '2023-10-25 08:12:00+00'),

    ('c0d00002-0000-4000-8000-000000000001',
     '11111111-1111-1111-1111-111111111111',
     'PAYMENT', '2023-10-01', '2023-12-31',
     'dddd0002-0000-0000-0000-000000000000',
     '{"paymentId":"dddd0002-0000-0000-0000-000000000000","amountEurCents":170300,"status":"PAID","paidAt":"2024-01-30"}',
     '2034-01-30', '2024-01-30 14:05:00+00')
ON CONFLICT DO NOTHING;

-- ── Demo access log entries ───────────────────────────────────────────────────

INSERT INTO record_access_log (id, record_id, accessor_type, accessor_id, purpose)
VALUES
    ('e0a00001-0000-4000-8000-000000000001',
     'c0b00001-0000-4000-8000-000000000001',
     'REGISTRANT', '11111111-1111-1111-1111-111111111111',
     'Registrant viewed Q3 2023 return record via taxable-person portal'),
    ('e0a00002-0000-4000-8000-000000000001',
     'c0b00001-0000-4000-8000-000000000001',
     'CASEWORKER', 'cc000000-0000-0000-0000-000000000001',
     'Caseworker reviewed Q3 2023 EU return during audit check')
ON CONFLICT DO NOTHING;
