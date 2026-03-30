-- V9001__seed_demo_data.sql  (registration-service)
-- Demo profile only — loaded via classpath:db/seed when SPRING_PROFILES_ACTIVE=demo.
-- UUIDs must match DemoConstants.java exactly.
-- Idempotent: INSERT ... ON CONFLICT DO NOTHING

SET search_path TO registration;

-- ── Registrant 1: Nina Hansen ApS — EU scheme, active ────────────────────────
INSERT INTO registrant (id, scheme_type, status, vat_number, country_code, legal_name,
                        address_line1, city, postal_code, contact_email,
                        binding_start, binding_end)
VALUES ('11111111-1111-1111-1111-111111111111', 'EU', 'ACTIVE',
        'DK12345678', 'DK', 'Nina Hansen ApS',
        'Strandvejen 42', 'København Ø', '2100', 'nina@ninahansen.dk',
        '2023-01-01', '2025-12-31')
ON CONFLICT DO NOTHING;

INSERT INTO scheme_registration (id, registrant_id, scheme_type, valid_from,
                                 notification_submitted_at)
VALUES ('sr000001-0000-0000-0000-000000000001',
        '11111111-1111-1111-1111-111111111111', 'EU', '2023-01-01',
        '2022-12-18 10:00:00+01')
ON CONFLICT DO NOTHING;

-- ── Registrant 2: Nordic Konsulent AS — Non-EU scheme, active ─────────────────
INSERT INTO registrant (id, scheme_type, status, vat_number, country_code, legal_name,
                        address_line1, city, postal_code, contact_email)
VALUES ('22222222-2222-2222-2222-222222222222', 'NON_EU', 'ACTIVE',
        'NO987654321MVA', 'NO', 'Nordic Konsulent AS',
        'Karl Johans gate 7', 'Oslo', '0154', 'kontakt@nordickonsulent.no')
ON CONFLICT DO NOTHING;

INSERT INTO scheme_registration (id, registrant_id, scheme_type, valid_from,
                                 notification_submitted_at)
VALUES ('sr000002-0000-0000-0000-000000000001',
        '22222222-2222-2222-2222-222222222222', 'NON_EU', '2022-07-01',
        '2022-06-20 09:00:00+02')
ON CONFLICT DO NOTHING;

-- ── Registrant 3: EasyProxy GmbH — Import scheme, active (intermediary) ───────
INSERT INTO registrant (id, scheme_type, status, vat_number, country_code, legal_name,
                        address_line1, city, postal_code, contact_email)
VALUES ('33333333-3333-3333-3333-333333333333', 'IMPORT', 'ACTIVE',
        'ATU12345678', 'AT', 'EasyProxy GmbH',
        'Mariahilfer Str. 88', 'Wien', '1070', 'oss@easyproxy.eu')
ON CONFLICT DO NOTHING;

INSERT INTO scheme_registration (id, registrant_id, scheme_type, valid_from,
                                 notification_submitted_at)
VALUES ('sr000003-0000-0000-0000-000000000001',
        '33333333-3333-3333-3333-333333333333', 'IMPORT', '2021-07-01',
        '2021-07-01 00:00:00+00')
ON CONFLICT DO NOTHING;

INSERT INTO intermediary (id, registrant_id, intermediary_vat, country_code)
VALUES ('44444444-4444-4444-4444-444444444444',
        '33333333-3333-3333-3333-333333333333', 'ATU12345678', 'AT')
ON CONFLICT DO NOTHING;

INSERT INTO principal (id, intermediary_id, principal_vat, country_code, legal_name)
VALUES ('55555555-5555-5555-5555-555555555555',
        '44444444-4444-4444-4444-444444444444', 'GB123456789', 'GB', 'QuickGoods Ltd')
ON CONFLICT DO NOTHING;
