-- V9001__seed_demo_data.sql  (payment-service)
-- Demo profile only — loaded via classpath:db/seed when SPRING_PROFILES_ACTIVE=demo.
-- ECB rates: last day of each reporting period (ML § 66e, 66n).
-- UUIDs must match DemoConstants.java exactly.
-- Idempotent: INSERT ... ON CONFLICT DO NOTHING

SET search_path TO payment;

-- ── ECB exchange rates (DKK→EUR, last day of period) ─────────────────────────
INSERT INTO ecb_exchange_rate (id, from_currency, to_currency, rate, rate_date) VALUES
    ('ecb00001-0000-0000-0000-000000000001', 'DKK', 'EUR', 0.1341397, '2023-09-30'),
    ('ecb00002-0000-0000-0000-000000000001', 'DKK', 'EUR', 0.1340200, '2023-12-31'),
    ('ecb00003-0000-0000-0000-000000000001', 'DKK', 'EUR', 0.1342100, '2024-03-31'),
    ('ecb00004-0000-0000-0000-000000000001', 'NOK', 'EUR', 0.0877000, '2023-09-30'),
    ('ecb00005-0000-0000-0000-000000000001', 'NOK', 'EUR', 0.0885000, '2023-12-31')
ON CONFLICT DO NOTHING;

-- ── Payments: EU scheme (Nina Hansen ApS) ────────────────────────────────────

-- Q3 2023 — paid
INSERT INTO payment (id, registrant_id, return_id, amount_eur_cents,
                     status, due_date, paid_at)
VALUES ('dddd0001-0000-0000-0000-000000000000',
        '11111111-1111-1111-1111-111111111111',
        'aaaa0001-0000-0000-0000-000000000000',
        204000, 'PAID', '2023-10-31', '2023-10-25 08:12:00+02')
ON CONFLICT DO NOTHING;

-- Q4 2023 — paid
INSERT INTO payment (id, registrant_id, return_id, amount_eur_cents,
                     status, due_date, paid_at)
VALUES ('dddd0002-0000-0000-0000-000000000000',
        '11111111-1111-1111-1111-111111111111',
        'aaaa0002-0000-0000-0000-000000000000',
        170300, 'PAID', '2024-01-31', '2024-01-30 14:05:00+01')
ON CONFLICT DO NOTHING;

-- Q1 2024 correction — pending (correction submitted; top-up payment due)
INSERT INTO payment (id, registrant_id, return_id, amount_eur_cents,
                     status, due_date)
VALUES ('dddd0003-0000-0000-0000-000000000000',
        '11111111-1111-1111-1111-111111111111',
        'aaaa0004-0000-0000-0000-000000000000',
        9500, 'PENDING', '2024-07-31')
ON CONFLICT DO NOTHING;

-- ── Payments: Non-EU scheme (Nordic Konsulent AS — NOK→EUR conversion) ────────

INSERT INTO payment (id, registrant_id, return_id, amount_eur_cents,
                     local_currency, amount_local_cents, ecb_rate_id,
                     status, due_date, paid_at)
VALUES ('eeee0001-0000-0000-0000-000000000000',
        '22222222-2222-2222-2222-222222222222',
        'bbbb0001-0000-0000-0000-000000000000',
        136000, 'NOK', 1551000,
        'ecb00004-0000-0000-0000-000000000001',
        'PAID', '2023-10-31', '2023-10-31 11:00:00+02')
ON CONFLICT DO NOTHING;

-- ── Payments: Import scheme (EasyProxy GmbH) ─────────────────────────────────

-- October 2023 — paid
INSERT INTO payment (id, registrant_id, return_id, amount_eur_cents,
                     status, due_date, paid_at)
VALUES ('ffff0001-0000-0000-0000-000000000000',
        '33333333-3333-3333-3333-333333333333',
        'cccc0001-0000-0000-0000-000000000000',
        38800, 'PAID', '2023-11-30', '2023-11-29 09:45:00+01')
ON CONFLICT DO NOTHING;
