package dk.osm2.registration.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for all registration-related endpoints.
 *
 * <p>Contains all fields accessed by {@code RegistrationSteps} step definitions
 * across all 49 Gherkin scenarios. Fields not applicable to a given state will be null.
 *
 * <p>Legal basis: ML §§ 66a–66j; Momsbekendtgørelsen §§ 115–119
 * <p>Petition: OSS-02 — FR-OSS-02-001 through FR-OSS-02-038
 */
public record RegistrationResponse(

        /** ID of the {@code SchemeRegistration} record. */
        UUID registrationId,

        /** ID of the {@code Registrant} record. */
        UUID registrantId,

        /** Current lifecycle status (e.g., "PENDING_VAT_NUMBER", "ACTIVE", "EXCLUDED"). */
        String status,

        /** Effective date of the registration (quarter-start or early-delivery date). */
        LocalDate effectiveDate,

        /** Assigned OSS VAT number (null until approval, ML § 66b stk. 2). */
        String vatNumber,

        /** OSS scheme type ("NON_EU", "EU", "IMPORT"). */
        String schemeType,

        /** Legal basis citation (ML § + Momsbekendtgørelsen reference). */
        String legalBasis,

        // ── Early-delivery exception (ML § 66b stk. 3) ───────────────────────

        /** True if the early-delivery exception was applied. */
        Boolean earlyDeliveryException,

        // ── VAT number processing flags ───────────────────────────────────────

        /** True if a delay notification was sent (day-8 rule, ML § 66b stk. 4). */
        Boolean delayNotificationSent,

        /** Expected VAT number assignment date (populated when delay notification sent). */
        LocalDate expectedAssignmentDate,

        /** VAT number flag ("PROVISIONAL" or "CONFIRMED"). */
        String vatNumberFlag,

        // ── EU scheme binding period (ML § 66d stk. 2) ───────────────────────

        /** End date of the binding period for EU scheme identification member state. */
        LocalDate bindingPeriodEnd,

        /** Binding rule type: "CASE_A", "CASE_B", or "CASE_C". */
        String bindingRuleType,

        // ── Change notification compliance ────────────────────────────────────

        /** True if a change notification was submitted within the 10-day window. */
        Boolean changeNotificationTimely,

        /** Compliance flags for late or irregular notifications (e.g., "LATE_NOTIFICATION"). */
        List<String> complianceFlags,

        // ── IMS change notifications ──────────────────────────────────────────

        /** True if the outgoing notification to the new identification member state was dispatched. */
        Boolean outgoingMemberStateNotificationDispatched,

        /** Date the Danish registration was closed (IMS change scenario). */
        LocalDate closedDate,

        // ── Deregistration fields ─────────────────────────────────────────────

        /** True if voluntary deregistration was submitted ≥15 days before quarter end. */
        Boolean deregistrationTimely,

        /** Effective date of deregistration. */
        LocalDate deregistrationEffectiveDate,

        /** Date until which re-registration is blocked (2-year ban, ML § 66j stk. 2). */
        LocalDate reRegistrationBlockUntil,

        // ── Exclusion fields ──────────────────────────────────────────────────

        /** The exclusion criterion applied (e.g., "PERSISTENT_NON_COMPLIANCE"). */
        String exclusionCriterion,

        /** Date the exclusion decision was issued and communicated (ML § 66j). */
        LocalDate exclusionDecisionDate,

        /** Effective date of exclusion. */
        LocalDate exclusionEffectiveDate,

        // ── Scheme-switch fields ──────────────────────────────────────────────

        /** Effective date of the new scheme registration after a scheme switch (OQ-5). */
        LocalDate newSchemeEffectiveDate,

        // ── Transitional provision fields ─────────────────────────────────────

        /** Date when the last identification information update was recorded. */
        LocalDate lastIdentificationUpdateDate

) {}
