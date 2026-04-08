-- V9001__seed_demo_data.sql  (return-service)
-- Demo profile only — loaded via classpath:db/seed when SPRING_PROFILES_ACTIVE=demo.
-- UUIDs must match DemoConstants.java exactly.
-- Idempotent: INSERT ... ON CONFLICT DO NOTHING

SET search_path TO return_svc;

-- ── EU scheme returns: Nina Hansen ApS ───────────────────────────────────────

-- Q3 2023 — filed and accepted
INSERT INTO vat_return (id, registrant_id, scheme_type, period_start, period_end,
                        deadline, status, nil_return, submitted_at)
VALUES ('aaaa0001-0000-0000-0000-000000000000',
        '11111111-1111-1111-1111-111111111111', 'EU',
        '2023-07-01', '2023-09-30', '2023-10-31', 'ACCEPTED', FALSE,
        '2023-10-20 09:15:00+02')
ON CONFLICT DO NOTHING;

INSERT INTO return_line (id, return_id, member_state,
                         taxable_amount_eur_cents, vat_rate_pct, vat_amount_eur_cents)
VALUES
    ('b0010001-0001-4000-8000-000000000001', 'aaaa0001-0000-0000-0000-000000000000',
     'DE', 500000, 19.00, 95000),
    ('b0010001-0001-4000-8000-000000000002', 'aaaa0001-0000-0000-0000-000000000000',
     'FR', 320000, 20.00, 64000),
    ('b0010001-0001-4000-8000-000000000003', 'aaaa0001-0000-0000-0000-000000000000',
     'SE', 180000, 25.00, 45000)
ON CONFLICT DO NOTHING;

-- Q4 2023 — filed and accepted
INSERT INTO vat_return (id, registrant_id, scheme_type, period_start, period_end,
                        deadline, status, nil_return, submitted_at)
VALUES ('aaaa0002-0000-0000-0000-000000000000',
        '11111111-1111-1111-1111-111111111111', 'EU',
        '2023-10-01', '2023-12-31', '2024-01-31', 'ACCEPTED', FALSE,
        '2024-01-28 14:03:00+01')
ON CONFLICT DO NOTHING;

INSERT INTO return_line (id, return_id, member_state,
                         taxable_amount_eur_cents, vat_rate_pct, vat_amount_eur_cents)
VALUES
    ('b0020001-0001-4000-8000-000000000001', 'aaaa0002-0000-0000-0000-000000000000',
     'DE', 620000, 19.00, 117800),
    ('b0020001-0001-4000-8000-000000000002', 'aaaa0002-0000-0000-0000-000000000000',
     'NL', 250000, 21.00, 52500)
ON CONFLICT DO NOTHING;

-- Q1 2024 original — superseded by correction
INSERT INTO vat_return (id, registrant_id, scheme_type, period_start, period_end,
                        deadline, status, nil_return, submitted_at)
VALUES ('aaaa0003-0000-0000-0000-000000000000',
        '11111111-1111-1111-1111-111111111111', 'EU',
        '2024-01-01', '2024-03-31', '2024-04-30', 'CORRECTED', FALSE,
        '2024-04-25 11:00:00+02')
ON CONFLICT DO NOTHING;

INSERT INTO return_line (id, return_id, member_state,
                         taxable_amount_eur_cents, vat_rate_pct, vat_amount_eur_cents)
VALUES
    ('b0030001-0001-4000-8000-000000000001', 'aaaa0003-0000-0000-0000-000000000000',
     'DE', 480000, 19.00, 91200)
ON CONFLICT DO NOTHING;

-- Q1 2024 correction (ML § 66n — corrects original within 3-year window)
INSERT INTO vat_return (id, registrant_id, scheme_type, period_start, period_end,
                        deadline, status, nil_return, submitted_at, corrects_return_id)
VALUES ('aaaa0004-0000-0000-0000-000000000000',
        '11111111-1111-1111-1111-111111111111', 'EU',
        '2024-01-01', '2024-03-31', '2024-04-30', 'ACCEPTED', FALSE,
        '2024-06-10 10:30:00+02', 'aaaa0003-0000-0000-0000-000000000000')
ON CONFLICT DO NOTHING;

INSERT INTO return_line (id, return_id, member_state,
                         taxable_amount_eur_cents, vat_rate_pct, vat_amount_eur_cents)
VALUES
    ('b0040001-0001-4000-8000-000000000001', 'aaaa0004-0000-0000-0000-000000000000',
     'DE', 530000, 19.00, 100700)
ON CONFLICT DO NOTHING;

-- ── Non-EU scheme returns: Nordic Konsulent AS ────────────────────────────────

-- Q3 2023 — accepted
INSERT INTO vat_return (id, registrant_id, scheme_type, period_start, period_end,
                        deadline, status, nil_return, submitted_at)
VALUES ('bbbb0001-0000-0000-0000-000000000000',
        '22222222-2222-2222-2222-222222222222', 'NON_EU',
        '2023-07-01', '2023-09-30', '2023-10-31', 'ACCEPTED', FALSE,
        '2023-10-29 16:45:00+02')
ON CONFLICT DO NOTHING;

INSERT INTO return_line (id, return_id, member_state,
                         taxable_amount_eur_cents, vat_rate_pct, vat_amount_eur_cents)
VALUES
    ('b0050001-0001-4000-8000-000000000001', 'bbbb0001-0000-0000-0000-000000000000',
     'DK', 400000, 25.00, 100000),
    ('b0050001-0001-4000-8000-000000000002', 'bbbb0001-0000-0000-0000-000000000000',
     'FI', 150000, 24.00, 36000)
ON CONFLICT DO NOTHING;

-- Q4 2023 — nil return (no supplies this quarter)
INSERT INTO vat_return (id, registrant_id, scheme_type, period_start, period_end,
                        deadline, status, nil_return, submitted_at)
VALUES ('bbbb0002-0000-0000-0000-000000000000',
        '22222222-2222-2222-2222-222222222222', 'NON_EU',
        '2023-10-01', '2023-12-31', '2024-01-31', 'NIL', TRUE,
        '2024-01-15 08:00:00+01')
ON CONFLICT DO NOTHING;

-- ── Import scheme returns: EasyProxy GmbH ────────────────────────────────────

-- October 2023 — accepted (monthly cadence, ML § 66u)
INSERT INTO vat_return (id, registrant_id, scheme_type, period_start, period_end,
                        deadline, status, nil_return, submitted_at)
VALUES ('cccc0001-0000-0000-0000-000000000000',
        '33333333-3333-3333-3333-333333333333', 'IMPORT',
        '2023-10-01', '2023-10-31', '2023-11-30', 'ACCEPTED', FALSE,
        '2023-11-28 12:00:00+01')
ON CONFLICT DO NOTHING;

INSERT INTO return_line (id, return_id, member_state,
                         taxable_amount_eur_cents, vat_rate_pct, vat_amount_eur_cents)
VALUES
    ('b0060001-0001-4000-8000-000000000001', 'cccc0001-0000-0000-0000-000000000000',
     'DE', 120000, 19.00, 22800),
    ('b0060001-0001-4000-8000-000000000002', 'cccc0001-0000-0000-0000-000000000000',
     'FR', 80000, 20.00, 16000)
ON CONFLICT DO NOTHING;

-- November 2023 — pending payment
INSERT INTO vat_return (id, registrant_id, scheme_type, period_start, period_end,
                        deadline, status, nil_return, submitted_at)
VALUES ('cccc0002-0000-0000-0000-000000000000',
        '33333333-3333-3333-3333-333333333333', 'IMPORT',
        '2023-11-01', '2023-11-30', '2023-12-31', 'ACCEPTED', FALSE,
        '2023-12-20 10:00:00+01')
ON CONFLICT DO NOTHING;

INSERT INTO return_line (id, return_id, member_state,
                         taxable_amount_eur_cents, vat_rate_pct, vat_amount_eur_cents)
VALUES
    ('b0070001-0001-4000-8000-000000000001', 'cccc0002-0000-0000-0000-000000000000',
     'AT', 95000, 20.00, 19000)
ON CONFLICT DO NOTHING;
