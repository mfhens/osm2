-- V9001__seed_demo_data.sql  (scheme-service)
-- Demo profile only — loaded via classpath:db/seed when SPRING_PROFILES_ACTIVE=demo.
-- Seed: eligibility rules for all three OSS schemes.
-- Idempotent: INSERT ... ON CONFLICT DO NOTHING

SET search_path TO scheme;

INSERT INTO eligibility_rule (id, scheme_code, rule_code, description_da) VALUES
    -- Non-EU scheme (ML §§ 66a-66f)
    ('e0000001-0000-0000-0000-000000000001', 'NON_EU', 'NOT_ESTABLISHED_EU',
     'Afgiftspligtig person må ikke have etableringssted i EU'),
    ('e0000001-0000-0000-0000-000000000002', 'NON_EU', 'NOT_REGISTERED_EU',
     'Afgiftspligtig person må ikke være momsregistreret i et EU-land'),
    ('e0000001-0000-0000-0000-000000000003', 'NON_EU', 'SERVICES_ONLY',
     'Ordningen dækker kun ydelser (ikke varer)'),

    -- EU scheme (ML §§ 66g-66p)
    ('e0000002-0000-0000-0000-000000000001', 'EU', 'ESTABLISHED_EU',
     'Afgiftspligtig person skal have etableringssted i EU'),
    ('e0000002-0000-0000-0000-000000000002', 'EU', 'DISTANCE_SALES_OR_SERVICES',
     'Ordningen dækker fjernsalg af varer og ydelser til forbrugere'),
    ('e0000002-0000-0000-0000-000000000003', 'EU', 'BINDING_PERIOD',
     'Registrering er bindende i registreringsåret + 2 efterfølgende kalenderår (ML § 66k)'),
    ('e0000002-0000-0000-0000-000000000004', 'EU', 'PLATFORM_LIABILITY',
     'Platforme kan være ansvarlige for moms på vegne af sælgere (ML § 66h)'),

    -- Import scheme (ML §§ 66q-66u)
    ('e0000003-0000-0000-0000-000000000001', 'IMPORT', 'GOODS_MAX_EUR_150',
     'Ordningen gælder kun for varer med en reel værdi på max 150 EUR (ML § 66q)'),
    ('e0000003-0000-0000-0000-000000000002', 'IMPORT', 'INTERMEDIARY_JOINT_LIABILITY',
     'Formidler hæfter solidarisk for momsen (ML § 66s)'),
    ('e0000003-0000-0000-0000-000000000003', 'IMPORT', 'MONTHLY_RETURNS',
     'Angivelser indgives månedligt (ikke kvartalsvis)'),
    ('e0000003-0000-0000-0000-000000000004', 'IMPORT', 'CASCADE_EXCLUSION',
     'Udelukkelse af formidler medfører udelukkelse af alle repræsenterede principaler')
ON CONFLICT DO NOTHING;
