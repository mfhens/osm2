package dk.osm2.registration.domain;

import dk.ufst.opendebt.common.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Re-registration ban tracking for excluded taxable persons.
 *
 * <p>2-year ban from the valid_from of the exclusion event (not decision date).
 * (ML § 66d stk. 3 / § 66m stk. 3 / § 66v stk. 3)
 *
 * <p>Legal basis: ML §§ 66d stk. 3, 66m stk. 3, 66v stk. 3
 * <p>Petition: OSS-02 — FR-OSS-02-034
 */
@Entity
@Table(name = "exclusion_ban")
@Getter
@Setter
public class ExclusionBan extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The registrant subject to the ban. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrant_id", nullable = false)
    private Registrant registrant;

    /** The scheme from which the registrant was excluded. */
    @Enumerated(EnumType.STRING)
    @Column(name = "scheme_type", nullable = false, columnDefinition = "scheme_type")
    private SchemeType schemeType;

    /**
     * The exclusion registration event that triggered the ban.
     * References scheme_registration.id.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exclusion_reg_id", nullable = false)
    private SchemeRegistration exclusionRegistration;

    /** The exclusion criterion that led to the ban. Added in V3. */
    @Column(name = "criterion", length = 100)
    private String criterion;

    /**
     * Date from which the ban is lifted.
     * Computed as exclusion valid_to + 2 years (valid time, not decision date).
     */
    @Column(name = "ban_lifted_at", nullable = false)
    private LocalDate banLiftedAt;
}
