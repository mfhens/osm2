workspace "osm2" "One Stop Moms 2 — Danish VAT One Stop Shop system implementing EU OSS schemes (Non-EU, EU, Import) per Council Directive 2006/112/EC as amended." {

    model {

        // --- Actors ---
        taxablePerson = person "Taxable Person / Intermediary" "Business or intermediary registered under an OSS scheme, filing VAT returns across EU member states." "User"
        caseworker = person "Skatteforvaltningen Caseworker" "SKAT caseworker managing OSS registrations, returns, and enforcement." "Operator"
        memberStateAuthority = person "Member State Tax Authority" "Foreign EU tax authority receiving VAT distributions and accessing shared records." "External User"

        // --- Software System ---
        osm2 = softwareSystem "osm2" "One Stop Moms 2: Danish VAT One Stop Shop administration system. Handles scheme eligibility classification, registration/deregistration, VAT return filing, payment processing, and 10-year record retention for all three OSS schemes." {

            // --- Core Backend Services ---
            schemeService = container "osm2-scheme-service" "Classifies taxable persons into Non-EU, EU, or Import OSS schemes per ML 66-66u. Uses Drools rules engine for statutory eligibility logic validated against Catala oracle." "Java 21 / Spring Boot 3.5 / Drools 9" "Service" {
                tags "port:8081"
            }

            registrationService = container "osm2-registration-service" "Manages OSS registration and deregistration lifecycle for all three schemes. PII silo: holds taxable person identity data (VAT numbers, addresses, contact details). All other services reference via registrant_id UUID only." "Java 21 / Spring Boot 3.5" "Service" {
                tags "port:8082" "pii:true"
            }

            returnService = container "osm2-return-service" "VAT return filing, corrections, and late-return reminders per OSS regulations. References taxable persons via registrant_id UUID only." "Java 21 / Spring Boot 3.5" "Service" {
                tags "port:8083" "pii:false"
            }

            paymentService = container "osm2-payment-service" "Payment processing, refunds, ECB rate conversion, and distribution to member-state CMS. Double-entry bookkeeping. Tamper-evident ledger in immudb." "Java 21 / Spring Boot 3.5" "Service" {
                tags "port:8084" "pii:false"
            }

            recordsService = container "osm2-records-service" "10-year statutory record retention with dual-access for IMS and CMS. Read-only after retention period expires." "Java 21 / Spring Boot 3.5" "Service" {
                tags "port:8085" "pii:false"
            }

            // --- Portal BFFs ---
            taxablePersonPortal = container "osm2-taxable-person-portal" "BFF portal for taxable persons and intermediaries. Thymeleaf UI with OIDC Authorization Code Flow via MitID/NemLog-in through Keycloak." "Java 21 / Spring Boot 3.5 / Thymeleaf" "Web Browser" {
                tags "port:8090"
            }

            authorityPortal = container "osm2-authority-portal" "BFF portal for Skatteforvaltningen caseworkers. Thymeleaf UI with OIDC Authorization Code Flow via OCES3 through Keycloak." "Java 21 / Spring Boot 3.5 / Thymeleaf" "Web Browser" {
                tags "port:8091"
            }

            // --- Data Stores ---
            postgresDb = container "PostgreSQL" "Primary relational database. One schema per service (scheme, registration, return_svc, payment, records). Managed by Flyway." "PostgreSQL 18 Alpine" "Database" {
                tags "pii:true"
            }

            immudbLedger = container "immudb" "Tamper-evident payment ledger. Cryptographic proof of inclusion for all payment journal entries." "immudb" "Database" {
                tags "pii:false"
            }
        }

        // --- External Systems ---
        keycloak = softwareSystem "Keycloak" "Central OIDC/OAuth2 identity provider. Proxies MitID/NemLog-in for taxable persons and OCES3 for caseworkers. Hosts the osm2 realm." "External System" {
            tags "internal" "third-party"
        }

        mitid = softwareSystem "MitID / NemLog-in" "Danish national digital identity solution for citizen and business authentication." "External System" {
            tags "external" "third-party"
        }

        ecbApi = softwareSystem "ECB Exchange Rate API" "European Central Bank daily reference exchange rate feed used for EUR conversion in payment processing." "External System" {
            tags "external" "public"
        }

        memberStateCms = softwareSystem "Member State CMS" "Central Management System of each EU member state's tax authority. Receives VAT distributions from osm2 payment-service." "External System" {
            tags "external" "third-party"
        }

        nemKonto = softwareSystem "NemKonto" "Danish government payment infrastructure. Used by osm2-payment-service to disburse VAT refunds and payments. mTLS with OCES3 certificate." "External System" {
            tags "external" "third-party"
        }

        fordringssystem = softwareSystem "SKATs Fordringssystem" "Danish debt enforcement system. Receives enforcement handover from osm2-payment-service for overdue accounts. mTLS with OCES3 certificate." "External System" {
            tags "external" "third-party"
        }

        otelCollector = softwareSystem "OTel Collector + Grafana Stack" "OpenTelemetry Collector, Tempo (traces), Loki (logs), Prometheus (metrics), Grafana (dashboards)." "External System" {
            tags "internal" "third-party"
        }

        // --- Relationships: Actors to System ---
        taxablePerson -> taxablePersonPortal "Registers, files returns, and monitors status via" "HTTPS / OIDC"
        caseworker -> authorityPortal "Manages registrations, reviews returns, and initiates enforcement via" "HTTPS / OIDC"
        memberStateAuthority -> recordsService "Accesses shared VAT records via" "HTTPS / REST (OCES3 mTLS)"

        // --- Relationships: Portals to Backend Services ---
        taxablePersonPortal -> schemeService "Checks scheme eligibility via" "HTTPS / REST / JWT"
        taxablePersonPortal -> registrationService "Submits registration and deregistration requests via" "HTTPS / REST / JWT"
        taxablePersonPortal -> returnService "Files and corrects VAT returns via" "HTTPS / REST / JWT"

        authorityPortal -> schemeService "Queries scheme classifications via" "HTTPS / REST / JWT"
        authorityPortal -> registrationService "Manages registrant lifecycle via" "HTTPS / REST / JWT"
        authorityPortal -> returnService "Reviews and approves returns via" "HTTPS / REST / JWT"
        authorityPortal -> paymentService "Initiates manual payment actions via" "HTTPS / REST / JWT"
        authorityPortal -> recordsService "Accesses statutory records via" "HTTPS / REST / JWT"

        // --- Relationships: Service-to-Service ---
        registrationService -> schemeService "Validates scheme eligibility on registration via" "HTTPS / REST / JWT (internal)"
        returnService -> registrationService "Resolves registrant status via" "HTTPS / REST / JWT (internal)"
        returnService -> paymentService "Triggers payment processing on return approval via" "HTTPS / REST / JWT (internal)"
        paymentService -> recordsService "Archives payment records for retention via" "HTTPS / REST / JWT (internal)"
        schemeService -> recordsService "Archives eligibility decisions for retention via" "HTTPS / REST / JWT (internal)"
        registrationService -> recordsService "Archives registration events for retention via" "HTTPS / REST / JWT (internal)"
        returnService -> recordsService "Archives filed returns for retention via" "HTTPS / REST / JWT (internal)"

        // --- Relationships: Services to Data Stores ---
        schemeService -> postgresDb "Reads/writes scheme data" "SQL / HikariCP"
        registrationService -> postgresDb "Reads/writes registration data (PII)" "SQL / HikariCP"
        returnService -> postgresDb "Reads/writes return data" "SQL / HikariCP"
        paymentService -> postgresDb "Reads/writes payment ledger" "SQL / HikariCP"
        recordsService -> postgresDb "Reads/writes retention records" "SQL / HikariCP"
        paymentService -> immudbLedger "Appends tamper-evident payment entries" "gRPC / immudb4j"

        // --- Relationships: External Systems ---
        taxablePersonPortal -> keycloak "Authenticates users via" "OIDC Authorization Code Flow"
        authorityPortal -> keycloak "Authenticates caseworkers via" "OIDC Authorization Code Flow"
        keycloak -> mitid "Delegates authentication to" "OIDC Federation"
        paymentService -> ecbApi "Fetches daily EUR exchange rates from" "HTTPS / REST"
        paymentService -> memberStateCms "Distributes VAT payments to" "HTTPS / REST (OCES3 mTLS)"
        paymentService -> nemKonto "Disburses refunds and payments via" "SOAP / mTLS (OCES3 — dk.ufst:oces3-certificate-parser)"
        paymentService -> fordringssystem "Hands over overdue accounts to" "SOAP / mTLS (OCES3 — dk.ufst:oces3-certificate-parser)"

        // --- Observability ---
        schemeService -> otelCollector "Exports traces, metrics, logs to" "OTLP / gRPC"
        registrationService -> otelCollector "Exports traces, metrics, logs to" "OTLP / gRPC"
        returnService -> otelCollector "Exports traces, metrics, logs to" "OTLP / gRPC"
        paymentService -> otelCollector "Exports traces, metrics, logs to" "OTLP / gRPC"
        recordsService -> otelCollector "Exports traces, metrics, logs to" "OTLP / gRPC"
        taxablePersonPortal -> otelCollector "Exports traces, metrics, logs to" "OTLP / gRPC"
        authorityPortal -> otelCollector "Exports traces, metrics, logs to" "OTLP / gRPC"

        // --- Auth: Services validate JWT with Keycloak ---
        schemeService -> keycloak "Validates JWT tokens against" "OIDC / JWKS endpoint"
        registrationService -> keycloak "Validates JWT tokens against" "OIDC / JWKS endpoint"
        returnService -> keycloak "Validates JWT tokens against" "OIDC / JWKS endpoint"
        paymentService -> keycloak "Validates JWT tokens against" "OIDC / JWKS endpoint"
        recordsService -> keycloak "Validates JWT tokens against" "OIDC / JWKS endpoint"
    }

    views {

        systemContext osm2 "SystemContext" "System context view — osm2 in relation to its users and external systems" {
            include *
            autoLayout lr
        }

        container osm2 "Containers" "Container view — all osm2 services, portals, and data stores" {
            include *
            autoLayout lr
        }

        // [TODO] Add deploymentEnvironment views once Kubernetes manifests are finalised.
        // deploymentEnvironment "Production" "Kubernetes" {
        //     include *
        //     autoLayout lr
        // }

        theme default
    }
}
