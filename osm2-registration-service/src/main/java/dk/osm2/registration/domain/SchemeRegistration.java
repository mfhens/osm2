package dk.osm2.registration.domain;

import dk.ufst.opendebt.common.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Bitemporal registration lifecycle event.
 *
 * <p>VALID TIME columns (gyldighedstid):
 * <ul>
 *   <li>{@code validFrom} — when the registration became effective in the real world (ML § 66b stk. 3)</li>
 *   <li>{@code validTo}   — when the registration ceased (NULL = still active)</li>
 * </ul>
 *
 * <p>TRANSACTION TIME is tracked automatically by Hibernate Envers _AUD tables.
 *
 * <p>Legal basis: ML §§ 66a–66j; Momsbekendtgørelsen §§ 115–119
 * <p>Petition: OSS-02 — FR-OSS-02-001 through FR-OSS-02-038
 */
@Entity
@Table(name = "scheme_registration")
@Audited
@Getter
@Setter
public class SchemeRegistration extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /** The registrant this registration belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrant_id", nullable = false)
    private Registrant registrant;

    /** OSS scheme type (NON_EU, EU, IMPORT). */
    @Enumerated(EnumType.STRING)
    @Column(name = "scheme_type", nullable = false, columnDefinition = "scheme_type")
    private SchemeType schemeType;

    /** Valid-time start — when registration became effective (gyldighedstid). */
    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    /** Valid-time end — NULL means still active. */
    @Column(name = "valid_to")
    private LocalDate validTo;

    /** When the taxable person submitted the notification to SKAT. */
    @Column(name = "notification_submitted_at")
    private LocalDateTime notificationSubmittedAt;

    /** Assigned OSS VAT number (set on approval, ML § 66b stk. 2). Added in V3. */
    @Column(name = "vat_number", length = 50)
    private String vatNumber;

    /** Free-text explanation for retroactive valid_from/valid_to values. */
    @Column(name = "change_reason")
    private String changeReason;

    /** ISO 3166-1 alpha-2 identification member state. Added in V3. */
    @Column(name = "identification_member_state", length = 2)
    private String identificationMemberState;

    /** True if early-delivery exception (ML § 66b stk. 3) was applied. Added in V3. */
    @Column(name = "early_delivery_exception", nullable = false)
    private boolean earlyDeliveryException = false;

    /** The first delivery date that triggered early-delivery exception. Added in V3. */
    @Column(name = "first_delivery_date")
    private LocalDate firstDeliveryDate;

    /** Lifecycle status of this specific registration. Added in V3. */
    @Column(name = "registration_status", length = 50)
    private String registrationStatus;

    /** Binding period end date (EU scheme, ML § 66d stk. 2). Added in V3. */
    @Column(name = "binding_period_end")
    private LocalDate bindingPeriodEnd;

    /** Binding rule type (CASE_A, CASE_B, CASE_C). Added in V3. */
    @Column(name = "binding_rule_type", length = 20)
    private String bindingRuleType;

    /** Whether change notification was submitted timely (within 10 days). Added in V3. */
    @Column(name = "change_notification_timely")
    private Boolean changeNotificationTimely;

    /** Whether a delay notification was sent to the taxable person. Added in V3. */
    @Column(name = "delay_notification_sent", nullable = false)
    private boolean delayNotificationSent = false;

    /** Expected VAT number assignment date (for delay notification). Added in V3. */
    @Column(name = "expected_assignment_date")
    private LocalDate expectedAssignmentDate;

    /** VAT number flag (e.g., "PROVISIONAL", "CONFIRMED"). Added in V3. */
    @Column(name = "vat_number_flag", length = 20)
    private String vatNumberFlag;

    /** Whether outgoing IMS notification was dispatched. Added in V3. */
    @Column(name = "outgoing_ims_notification_dispatched", nullable = false)
    private boolean outgoingMemberStateNotificationDispatched = false;

    /** Registration closure date (when DK closed the record). Added in V3. */
    @Column(name = "closed_date")
    private LocalDate closedDate;

    /** Whether voluntary deregistration was timely (≥15 days before quarter end). Added in V3. */
    @Column(name = "deregistration_timely")
    private Boolean deregistrationTimely;

    /** Effective date of deregistration. Added in V3. */
    @Column(name = "deregistration_effective_date")
    private LocalDate deregistrationEffectiveDate;

    /** Re-registration block end date (2-year ban after PERSISTENT_NON_COMPLIANCE). Added in V3. */
    @Column(name = "re_registration_block_until")
    private LocalDate reRegistrationBlockUntil;

    /** Exclusion criterion applied. Added in V3. */
    @Column(name = "exclusion_criterion", length = 100)
    private String exclusionCriterion;

    /** Date exclusion decision was made and sent. Added in V3. */
    @Column(name = "exclusion_decision_date")
    private LocalDate exclusionDecisionDate;

    /** Effective date of exclusion. Added in V3. */
    @Column(name = "exclusion_effective_date")
    private LocalDate exclusionEffectiveDate;

    /** Effective date of the new scheme registration created during a scheme switch (FR-OSS-02-037). Added in V4. */
    @Column(name = "new_scheme_effective_date")
    private LocalDate newSchemeEffectiveDate;

    /** True when the transitional identification update is overdue (FR-OSS-02-038). Added in V4. */
    @Column(name = "transitional_update_overdue", nullable = false)
    private boolean transitionalUpdateOverdue = false;

    /** Date of the last submitted identification update under post-July-2021 rules. Added in V4. */
    @Column(name = "last_identification_update_date")
    private LocalDate lastIdentificationUpdateDate;
}
