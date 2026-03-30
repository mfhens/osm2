-- osm2 PostgreSQL initialisation script
-- Creates one database + user per service (least-privilege isolation)
-- Run automatically by postgres Docker container on first start.

-- Keycloak database
CREATE USER osm2_keycloak WITH PASSWORD 'osm2_keycloak';
CREATE DATABASE osm2_keycloak OWNER osm2_keycloak;
GRANT ALL PRIVILEGES ON DATABASE osm2_keycloak TO osm2_keycloak;

-- Scheme service
CREATE USER osm2_scheme WITH PASSWORD 'osm2_scheme';
CREATE DATABASE osm2_scheme OWNER osm2_scheme;
GRANT ALL PRIVILEGES ON DATABASE osm2_scheme TO osm2_scheme;

-- Registration service (PII silo)
CREATE USER osm2_registration WITH PASSWORD 'osm2_registration';
CREATE DATABASE osm2_registration OWNER osm2_registration;
GRANT ALL PRIVILEGES ON DATABASE osm2_registration TO osm2_registration;

-- Return service
CREATE USER osm2_return WITH PASSWORD 'osm2_return';
CREATE DATABASE osm2_return OWNER osm2_return;
GRANT ALL PRIVILEGES ON DATABASE osm2_return TO osm2_return;

-- Payment service
CREATE USER osm2_payment WITH PASSWORD 'osm2_payment';
CREATE DATABASE osm2_payment OWNER osm2_payment;
GRANT ALL PRIVILEGES ON DATABASE osm2_payment TO osm2_payment;

-- Records service
CREATE USER osm2_records WITH PASSWORD 'osm2_records';
CREATE DATABASE osm2_records OWNER osm2_records;
GRANT ALL PRIVILEGES ON DATABASE osm2_records TO osm2_records;
