package dk.osm2.registration.domain;

import dk.ufst.opendebt.common.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Core PII entity representing a taxable person registered for an OSS scheme.
 *
 * <p>PII SILO — identity data lives exclusively in this service.
 * All other services reference taxable persons via registrant_id UUID only.
 *
 * <p>Bitemporality tracked via Hibernate Envers {@code _AUD} tables (transaction time)
 * and {@code SchemeRegistration.validFrom/validTo} columns (valid time).
 *
 * <p>Legal basis: ML §§ 66a–66j; Momsbekendtgørelsen §§ 115–119
 * <p>Petition: OSS-02 — FR-OSS-02-001 through FR-OSS-02-038
 */
@Entity
@Table(name = "registrant")
@Audited
@Getter
@Setter
public class Registrant extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** Legal name of the taxable person (maps to legal_name in V1 schema). */
    @Column(name = "legal_name", nullable = false, length = 200)
    private String registrantName;

    /** ISO 3166-1 alpha-2 home country (maps to country_code in V1 schema). */
    @Column(name = "country_code", nullable = false, length = 2)
    private String homeCountry;

    /** Tax registration number in home country (added in V3 migration). */
    @Column(name = "home_country_tax_number", length = 100)
    private String homeCountryTaxNumber;

    /** Full postal address (maps to address_line1 in V1 schema). */
    @Column(name = "address_line1", length = 200)
    private String postalAddress;

    /** Contact email address (maps to contact_email in V1 schema). */
    @Column(name = "contact_email", length = 200)
    private String email;

    /** Contact phone number (maps to contact_phone in V1 schema). */
    @Column(name = "contact_phone", length = 50)
    private String phoneNumber;

    /** Bank account details for VAT refunds (added in V3 migration). */
    @Column(name = "bank_details")
    private String bankDetails;

    /** ISO 3166-1 alpha-2 identification member state (added in V3 migration). */
    @Column(name = "identification_member_state", length = 2, nullable = false)
    private String identificationMemberState;

    /**
     * OSS scheme type for this registration.
     * Stored as PostgreSQL {@code scheme_type} enum.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "scheme_type", nullable = false, columnDefinition = "scheme_type")
    private SchemeType schemeType;

    /**
     * Lifecycle status of this registration.
     * Stored as PostgreSQL {@code registrant_status} enum.
     * ML § 66b stk. 2 — initial status is PENDING_VAT_NUMBER.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "registrant_status")
    private RegistrantStatus status;

    /** Assigned OSS VAT number — nullable until approval (ML § 66b stk. 2). */
    @Column(name = "vat_number", length = 50)
    private String vatNumber;

    /** All scheme registrations for this registrant. */
    @OneToMany(mappedBy = "registrant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SchemeRegistration> schemeRegistrations = new ArrayList<>();
}
