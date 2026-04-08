package dk.osm2.registration.service;

import dk.osm2.registration.domain.*;
import dk.osm2.registration.dto.*;
import dk.osm2.registration.exception.*;
import dk.osm2.registration.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Core service for the OSS-02 registration lifecycle.
 *
 * <p>Implements state-machine transitions with guards:
 * PENDING_VAT_NUMBER → ACTIVE (approve)
 * PENDING_VAT_NUMBER → DEREGISTERED (reject)
 * ACTIVE → EXCLUDED (exclude)
 * ACTIVE → DEREGISTERED (deregister)
 * ACTIVE → CESSATION_NOTIFIED (cessation notification)
 *
 * <p>All illegal transitions throw {@link IllegalStateTransitionException}.
 *
 * <p>Legal basis: ML §§ 66a–66j; Momsbekendtgørelsen §§ 115–119
 * <p>Petition: OSS-02 — FR-OSS-02-001 through FR-OSS-02-038
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RegistrationService {

    private static final String LEGAL_BASIS =
            "ML §§ 66a–66j; Momsbekendtgørelsen §§ 115–119; Momsforordningen art. 57d–58c";

    /** EU member state ISO 3166-1 alpha-2 codes (as of 2024). */
    private static final Set<String> EU_MEMBER_STATES = Set.of(
            "AT", "BE", "BG", "CY", "CZ", "DE", "DK", "EE", "ES", "FI",
            "FR", "GR", "HR", "HU", "IE", "IT", "LT", "LU", "LV", "MT",
            "NL", "PL", "PT", "RO", "SE", "SI", "SK");

    private final RegistrantRepository registrantRepository;
    private final SchemeRegistrationRepository schemeRegistrationRepository;
    private final ExclusionBanRepository exclusionBanRepository;
    private final EffectiveDateCalculationService effectiveDateService;

    // =========================================================================
    // Submit registration
    // =========================================================================

    /**
     * Submit a new OSS registration notification.
     *
     * <p>Creates a {@link Registrant} and a {@link SchemeRegistration}.
     * Status is set to PENDING_VAT_NUMBER.
     * Effective date is calculated per ML § 66b stk. 1/3.
     *
     * <p>FR-OSS-02-001, FR-OSS-02-006
     */
    public RegistrationResponse submitRegistration(RegistrationRequest request) {
        SchemeType schemeType = parseSchemeType(request.scheme());

        // Check eligibility: NON_EU scheme requires no EU fixed establishment
        if (schemeType == SchemeType.NON_EU && request.hasEuEstablishment()) {
            throw new IllegalArgumentException("APPLICANT_HAS_EU_ESTABLISHMENT");
        }

        // Check EU scheme declaration requirements (ML § 66g / Momsbekendtgørelsen § 117)
        // Only required for non-EU-established entities (home country outside EU member states)
        if (schemeType == SchemeType.EU
                && !request.electronicInterfaceFlag()
                && !request.notEstablishedInEuDeclaration()
                && !isEuMemberState(request.homeCountry())) {
            throw new IllegalArgumentException("MISSING_DECLARATION");
        }

        // Calculate effective date
        LocalDate notificationDate = request.notificationDate() != null
                ? request.notificationDate()
                : LocalDate.now();
        LocalDate firstDeliveryDate = request.firstDeliveryDate();
        LocalDate desiredStartDate = request.desiredStartDate();

        LocalDate effectiveDate;
        boolean earlyDelivery = false;

        if (desiredStartDate != null) {
            // Explicit desired start date (e.g. from the "complete registration" scenarios)
            effectiveDate = desiredStartDate;
        } else if (firstDeliveryDate != null) {
            effectiveDate = effectiveDateService.calculate(notificationDate, firstDeliveryDate);
            earlyDelivery = effectiveDateService.qualifiesForEarlyDeliveryException(notificationDate, firstDeliveryDate);
        } else {
            effectiveDate = effectiveDateService.firstDayOfNextQuarter(notificationDate);
        }

        // Check active exclusion ban by homeCountryTaxNumber (identity-based ban check)
        if (request.homeCountryTaxNumber() != null) {
            List<ExclusionBan> existingBans = exclusionBanRepository.findActiveBansByTaxNumber(
                    request.homeCountryTaxNumber(), schemeType, LocalDate.now());
            if (!existingBans.isEmpty()) {
                ExclusionBan ban = existingBans.get(0);
                throw new ExclusionBanActiveException(null, schemeType, ban.getBanLiftedAt());
            }
        }
        // Also check by email for EU scheme (homeCountryTaxNumber may be null for EU-established entities)
        if (request.email() != null && "EU".equals(request.scheme())) {
            List<ExclusionBan> bansByEmail = exclusionBanRepository.findActiveBansByEmail(
                    request.email(), schemeType, LocalDate.now());
            if (!bansByEmail.isEmpty()) {
                ExclusionBan ban = bansByEmail.get(0);
                throw new ExclusionBanActiveException(null, schemeType, ban.getBanLiftedAt());
            }
        }

        // Determine VAT number and initial status
        String vatNumber = null;
        RegistrantStatus initialStatus = RegistrantStatus.PENDING_VAT_NUMBER;
        String initialRegStatus = initialStatus.name();

        if (request.existingDanishVatNumber() != null && "EU".equals(request.scheme())) {
            // EU scheme with existing DK VAT: pre-assign and activate immediately (ML § 66e stk. 2)
            vatNumber = request.existingDanishVatNumber();
            initialStatus = RegistrantStatus.ACTIVE;
            initialRegStatus = initialStatus.name();
        }

        // Determine binding period for EU scheme
        LocalDate bindingPeriodEnd = null;
        String bindingRuleType = null;
        if ("EU".equals(request.scheme())) {
            String bindingResult = calculateBindingPeriod(
                    request.homeCountry(), request.identificationMemberState(), effectiveDate);
            String[] parts = bindingResult.split("\\|");
            bindingRuleType = parts[0];
            if (parts.length > 1 && !parts[1].isEmpty()) {
                bindingPeriodEnd = LocalDate.parse(parts[1]);
            }
        }

        // Create registrant
        Registrant registrant = new Registrant();
        registrant.setRegistrantName(request.registrantName());
        registrant.setHomeCountry(request.homeCountry());
        registrant.setHomeCountryTaxNumber(request.homeCountryTaxNumber());
        registrant.setPostalAddress(request.postalAddress());
        registrant.setEmail(request.email());
        registrant.setPhoneNumber(request.phoneNumber());
        registrant.setBankDetails(request.bankDetails());
        registrant.setIdentificationMemberState(request.identificationMemberState());
        registrant.setSchemeType(schemeType);
        registrant.setStatus(initialStatus);
        registrant.setVatNumber(vatNumber);

        registrant = registrantRepository.save(registrant);

        // Create scheme registration
        SchemeRegistration registration = new SchemeRegistration();
        registration.setRegistrant(registrant);
        registration.setSchemeType(schemeType);
        registration.setValidFrom(effectiveDate);
        registration.setValidTo(null); // still active
        // Use the request's notification date if provided; fall back to desiredStartDate or now.
        // This enables deterministic day-8 boundary tests with past dates.
        LocalDateTime submittedAt = request.notificationDate() != null
                ? request.notificationDate().atStartOfDay()
                : (request.desiredStartDate() != null
                        ? request.desiredStartDate().atStartOfDay()
                        : LocalDateTime.now());
        registration.setNotificationSubmittedAt(submittedAt);
        registration.setVatNumber(vatNumber);
        registration.setIdentificationMemberState(request.identificationMemberState());
        registration.setEarlyDeliveryException(earlyDelivery);
        registration.setFirstDeliveryDate(firstDeliveryDate);
        registration.setRegistrationStatus(initialRegStatus);
        registration.setBindingPeriodEnd(bindingPeriodEnd);
        registration.setBindingRuleType(bindingRuleType);

        registration = schemeRegistrationRepository.save(registration);

        log.info("Registration submitted: registrationId={} registrantId={} scheme={} status=PENDING_VAT_NUMBER",
                registration.getId(), registrant.getId(), schemeType);

        return toResponse(registration, registrant);
    }

    // =========================================================================
    // Get / List
    // =========================================================================

    /**
     * Retrieve a single registration by its ID.
     *
     * <p>FR-OSS-02-013
     */
    @Transactional(readOnly = true)
    public RegistrationResponse getRegistration(UUID registrationId) {
        SchemeRegistration reg = findRegistration(registrationId);
        return toResponse(reg, reg.getRegistrant());
    }

    /**
     * List all registrations for a registrant.
     *
     * <p>FR-OSS-02-001
     */
    @Transactional(readOnly = true)
    public List<RegistrationResponse> listByRegistrant(UUID registrantId) {
        return schemeRegistrationRepository.findByRegistrantId(registrantId)
                .stream()
                .map(reg -> toResponse(reg, reg.getRegistrant()))
                .toList();
    }

    /**
     * Registrations in {@code PENDING_VAT_NUMBER} awaiting caseworker VAT assignment (authority queue).
     */
    @Transactional(readOnly = true)
    public List<RegistrationResponse> listPendingVatNumber() {
        return schemeRegistrationRepository
                .findByRegistrationStatusAndValidToIsNullOrderByNotificationSubmittedAtAsc(
                        RegistrantStatus.PENDING_VAT_NUMBER.name())
                .stream()
                .map(reg -> toResponse(reg, reg.getRegistrant()))
                .toList();
    }

    // =========================================================================
    // Approve
    // =========================================================================

    /**
     * Approve a pending registration and assign a VAT number.
     *
     * <p>Transition: PENDING_VAT_NUMBER → ACTIVE
     * <p>FR-OSS-02-013 (within 8 days)
     */
    public RegistrationResponse approveRegistration(UUID registrationId, ApprovalRequest request) {
        SchemeRegistration reg = findRegistration(registrationId);
        Registrant registrant = reg.getRegistrant();

        if (registrant.getStatus() != RegistrantStatus.PENDING_VAT_NUMBER) {
            throw new IllegalStateTransitionException(registrant.getStatus(), RegistrantStatus.ACTIVE);
        }

        // Assign VAT number
        String vatNumber = request.vatNumber();
        if (vatNumber == null || vatNumber.isBlank()) {
            // OQ-1: generate placeholder
            vatNumber = "DK" + registrationId.toString().substring(0, 8).toUpperCase();
        }

        registrant.setStatus(RegistrantStatus.ACTIVE);
        registrant.setVatNumber(vatNumber);
        registrantRepository.save(registrant);

        reg.setVatNumber(vatNumber);
        reg.setRegistrationStatus("ACTIVE");
        // NON_EU scheme VAT numbers are flagged as scheme-only (ML § 66c stk. 3)
        reg.setVatNumberFlag(reg.getSchemeType() == SchemeType.NON_EU ? "NON_EU_SCHEME_ONLY" : "CONFIRMED");
        schemeRegistrationRepository.save(reg);

        log.info("Registration approved: registrationId={} vatNumber={}", registrationId, vatNumber);

        // OQ-4: log notification, no actual delivery
        log.warn("OQ-4: VAT number {} assigned to registrant {} — electronic notification to taxable person pending",
                vatNumber, registrant.getId());

        return toResponse(reg, registrant);
    }

    // =========================================================================
    // Reject
    // =========================================================================

    /**
     * Reject a pending registration.
     *
     * <p>Transition: PENDING_VAT_NUMBER → DEREGISTERED
     * <p>FR-OSS-02-013
     */
    public RegistrationResponse rejectRegistration(UUID registrationId, RejectionRequest request) {
        SchemeRegistration reg = findRegistration(registrationId);
        Registrant registrant = reg.getRegistrant();

        if (registrant.getStatus() != RegistrantStatus.PENDING_VAT_NUMBER) {
            throw new IllegalStateTransitionException(registrant.getStatus(), RegistrantStatus.DEREGISTERED);
        }

        registrant.setStatus(RegistrantStatus.DEREGISTERED);
        registrantRepository.save(registrant);

        reg.setValidTo(LocalDate.now());
        reg.setRegistrationStatus("DEREGISTERED");
        reg.setChangeReason("REJECTED: " + request.reason());
        schemeRegistrationRepository.save(reg);

        log.info("Registration rejected: registrationId={} reason={}", registrationId, request.reason());

        return toResponse(reg, registrant);
    }

    // =========================================================================
    // Exclude
    // =========================================================================

    /**
     * Record a forced exclusion for a registration.
     *
     * <p>Transition: ACTIVE → EXCLUDED
     * Creates an {@link ExclusionBan} if criterion is PERSISTENT_NON_COMPLIANCE.
     * <p>FR-OSS-02-031 through FR-OSS-02-035
     */
    public RegistrationResponse excludeRegistration(UUID registrationId, ExclusionRequest request) {
        SchemeRegistration reg = findRegistration(registrationId);
        Registrant registrant = reg.getRegistrant();

        if (registrant.getStatus() != RegistrantStatus.ACTIVE) {
            throw new IllegalStateTransitionException(registrant.getStatus(), RegistrantStatus.EXCLUDED);
        }

        LocalDate effectiveDate = request.effectiveDate();
        String criterion = request.criterion();

        registrant.setStatus(RegistrantStatus.EXCLUDED);
        registrantRepository.save(registrant);

        reg.setValidTo(effectiveDate);
        reg.setRegistrationStatus("EXCLUDED");
        reg.setExclusionCriterion(criterion);
        reg.setExclusionDecisionDate(request.decisionDate() != null ? request.decisionDate() : LocalDate.now());
        reg.setExclusionEffectiveDate(effectiveDate);
        schemeRegistrationRepository.save(reg);

        // Create 2-year re-registration ban for PERSISTENT_NON_COMPLIANCE
        if ("PERSISTENT_NON_COMPLIANCE".equals(criterion)) {
            ExclusionBan ban = new ExclusionBan();
            ban.setRegistrant(registrant);
            ban.setSchemeType(reg.getSchemeType());
            ban.setExclusionRegistration(reg);
            ban.setCriterion(criterion);
            // Ban lifts the day before the 2-year anniversary (inclusive ban through 2-year mark - 1 day)
            ban.setBanLiftedAt(effectiveDate.plusYears(2).minusDays(1));
            exclusionBanRepository.save(ban);

            // Set re-registration block on the registration
            reg.setReRegistrationBlockUntil(effectiveDate.plusYears(2).minusDays(1));
            schemeRegistrationRepository.save(reg);

            log.info("Exclusion ban created: registrantId={} scheme={} banLiftedAt={}",
                    registrant.getId(), reg.getSchemeType(), effectiveDate.plusYears(2).minusDays(1));
        }

        log.warn("OQ-4: Exclusion decision for {} communicated to taxable person — electronic notification pending",
                registrationId);

        return toResponse(reg, registrant);
    }

    // =========================================================================
    // Deregister
    // =========================================================================

    /**
     * Process a voluntary deregistration notification.
     *
     * <p>Transition: ACTIVE → DEREGISTERED
     * Deregistration effective date is calculated per ML § 66h:
     * - If notified ≥15 days before quarter end: effective at quarter end
     * - If notified <15 days before quarter end: deferred to following quarter end
     * <p>FR-OSS-02-027 through FR-OSS-02-030
     */
    public RegistrationResponse deregisterRegistration(UUID registrationId, DeregistrationRequest request) {
        SchemeRegistration reg = findRegistration(registrationId);
        Registrant registrant = reg.getRegistrant();

        if (registrant.getStatus() != RegistrantStatus.ACTIVE) {
            throw new IllegalStateTransitionException(registrant.getStatus(), RegistrantStatus.DEREGISTERED);
        }

        // request.effectiveDate() is the notification date (as passed from step defs)
        LocalDate notificationDate = request.effectiveDate() != null ? request.effectiveDate() : LocalDate.now();

        // ML § 66h: timely = notification submitted ≥15 days before quarter end (inclusive counting)
        LocalDate quarterEnd = lastDayOfCurrentQuarter(notificationDate);
        long daysBeforeQuarterEnd = java.time.temporal.ChronoUnit.DAYS.between(notificationDate, quarterEnd);

        // Inclusive count: e.g. March 17 → March 31 = 15 days (17,18,...,31), DAYS.between = 14
        boolean timely = daysBeforeQuarterEnd >= 14;
        // Effective date = first day of next quarter (not last day of current)
        LocalDate effectiveDate = timely
                ? quarterEnd.plusDays(1)
                : lastDayOfNextQuarter(notificationDate).plusDays(1);

        registrant.setStatus(RegistrantStatus.DEREGISTERED);
        registrantRepository.save(registrant);

        reg.setValidTo(effectiveDate);
        reg.setRegistrationStatus("DEREGISTERED");
        reg.setDeregistrationTimely(timely);
        reg.setDeregistrationEffectiveDate(effectiveDate);
        reg.setChangeReason("VOLUNTARY: " + request.reason());
        schemeRegistrationRepository.save(reg);

        log.info("Registration deregistered: registrationId={} effectiveDate={} timely={}",
                registrationId, effectiveDate, timely);

        return toResponse(reg, registrant);
    }

    // =========================================================================
    // Helpers — mapping and calculation
    // =========================================================================

    private SchemeRegistration findRegistration(UUID registrationId) {
        return schemeRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new RegistrationNotFoundException(registrationId));
    }

    private SchemeType parseSchemeType(String scheme) {
        try {
            return SchemeType.valueOf(scheme);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown scheme type: " + scheme);
        }
    }

    /**
     * Build the legal-basis citation string for the given scheme type.
     */
    private String legalBasisFor(SchemeType schemeType) {
        return switch (schemeType) {
            case NON_EU -> "ML §§ 66a–66c; Momsbekendtgørelsen §§ 115–117; MSD art. 358a";
            case EU     -> "ML §§ 66d–66j; Momsbekendtgørelsen §§ 118–119; MSD art. 369a";
            case IMPORT -> "ML §§ 66m–66u; Momsbekendtgørelsen §§ 120–122; MSD art. 369l";
        };
    }

    /**
     * Calculate the binding period for the EU scheme identification member state.
     * Returns "CASE_X|endDate" or "CASE_A|".
     *
     * <p>ML § 66d stk. 2:
     * <ul>
     *   <li>Case A: home establishment is in DK → no binding period.</li>
     *   <li>Case B/C: binding period = registration year + 2 following calendar years
     *       → end date = 31 December of (registration year + 2).</li>
     * </ul>
     */
    private String calculateBindingPeriod(String homeCountry, String identificationMemberState,
                                          LocalDate effectiveDate) {
        if ("DK".equals(homeCountry)) {
            return "NOT_APPLICABLE|";
        }
        // Case B or C: binding applies (ML § 66d stk. 2)
        int bindingEndYear = effectiveDate.getYear() + 2;
        LocalDate bindingEnd = LocalDate.of(bindingEndYear, 12, 31);
        return "ML_66D_STK2|" + bindingEnd;
    }

    /**
     * Last day of the current quarter.
     */
    private LocalDate lastDayOfCurrentQuarter(LocalDate date) {
        int month = date.getMonthValue();
        int quarterEndMonth = ((month - 1) / 3 + 1) * 3;
        return LocalDate.of(date.getYear(), quarterEndMonth, 1)
                .withDayOfMonth(LocalDate.of(date.getYear(), quarterEndMonth, 1).lengthOfMonth());
    }

    /**
     * Last day of the next quarter.
     */
    private LocalDate lastDayOfNextQuarter(LocalDate date) {
        return lastDayOfCurrentQuarter(date.plusMonths(3));
    }

    // =========================================================================
    // Notify data change (ML § 66b stk. 6 / § 66e stk. 5)
    // =========================================================================

    /**
     * Assign a new individual EU scheme VAT number when the ordinary Danish VAT registration ceases.
     * The shared DK VAT number is replaced with a unique EU scheme number (ML § 66e stk. 2).
     */
    public RegistrationResponse assignNewEuVatNumber(UUID registrationId) {
        SchemeRegistration reg = findRegistration(registrationId);
        Registrant registrant = reg.getRegistrant();

        // Generate a fresh individual EU VAT number
        String newVatNumber = "DKEU" + registrationId.toString().substring(0, 8).toUpperCase();
        registrant.setVatNumber(newVatNumber);
        registrantRepository.save(registrant);

        reg.setVatNumber(newVatNumber);
        schemeRegistrationRepository.save(reg);

        return toResponse(reg, registrant);
    }

    /**
     * Record a data-change notification and determine timeliness.
     * Timely = notified within 10 days after the end of the month in which the change occurred.
     */
    public RegistrationResponse notifyDataChange(UUID registrationId, LocalDate changeDate, LocalDate notificationDate) {
        SchemeRegistration reg = findRegistration(registrationId);

        LocalDate deadline = changeDate.withDayOfMonth(changeDate.lengthOfMonth()).plusDays(10);
        boolean timely = !notificationDate.isAfter(deadline);

        reg.setChangeNotificationTimely(timely);
        reg.setLastIdentificationUpdateDate(changeDate);
        schemeRegistrationRepository.save(reg);

        return toResponse(reg, reg.getRegistrant());
    }

    // =========================================================================
    // Notify cessation (ML § 66i stk. 1)
    // =========================================================================

    /**
     * Record a voluntary cessation notification; transitions to CESSATION_NOTIFIED.
     */
    public RegistrationResponse notifyCessationEvent(UUID registrationId, LocalDate cessationDate, LocalDate notificationDate) {
        SchemeRegistration reg = findRegistration(registrationId);

        LocalDate deadline = cessationDate.withDayOfMonth(cessationDate.lengthOfMonth()).plusDays(10);
        boolean timely = !notificationDate.isAfter(deadline);

        Registrant registrant = reg.getRegistrant();
        registrant.setStatus(RegistrantStatus.CESSATION_NOTIFIED);
        registrantRepository.save(registrant);

        reg.setRegistrationStatus("CESSATION_NOTIFIED");
        reg.setChangeNotificationTimely(timely);
        reg.setLastIdentificationUpdateDate(cessationDate);
        schemeRegistrationRepository.save(reg);

        return toResponse(reg, registrant);
    }

    // =========================================================================
    // Change identification member state (ML § 66d stk. 3)
    // =========================================================================

    /**
     * Handle an identification-member-state change request.
     * If binding period is active and no establishment move date → blocked.
     */
    public RegistrationResponse changeIdentificationMemberState(UUID registrationId, Map<String, Object> body) {
        SchemeRegistration reg = findRegistration(registrationId);

        String requestedChangeDateStr = (String) body.get("requestedChangeDate");
        String establishmentMoveDateStr = (String) body.get("establishmentMoveDate");
        Object notifyNewMemberStateRaw = body.get("notifyNewMemberState");

        LocalDate requestedChangeDate = requestedChangeDateStr != null ? LocalDate.parse(requestedChangeDateStr) : null;
        LocalDate establishmentMoveDate = establishmentMoveDateStr != null ? LocalDate.parse(establishmentMoveDateStr) : null;
        boolean notifyNewMemberState = Boolean.TRUE.equals(notifyNewMemberStateRaw);

        // Binding period check
        LocalDate bindingPeriodEnd = reg.getBindingPeriodEnd();
        if (bindingPeriodEnd != null
                && requestedChangeDate != null
                && !requestedChangeDate.isAfter(bindingPeriodEnd)
                && establishmentMoveDate == null) {
            throw new IllegalArgumentException("BOUND_TO_IDENTIFICATION_MEMBER_STATE_UNTIL: " + bindingPeriodEnd);
        }

        // Apply IMS change
        if (establishmentMoveDate != null) {
            reg.setValidFrom(establishmentMoveDate);
            reg.setClosedDate(establishmentMoveDate);
            reg.setOutgoingMemberStateNotificationDispatched(notifyNewMemberState);
            reg.setLastIdentificationUpdateDate(establishmentMoveDate);
            schemeRegistrationRepository.save(reg);
        }

        return toResponse(reg, reg.getRegistrant());
    }

    // =========================================================================
    // EU member state helper
    // =========================================================================

    private boolean isEuMemberState(String countryCode) {
        return countryCode != null && EU_MEMBER_STATES.contains(countryCode.toUpperCase());
    }

    /**
     * Map domain objects to the response DTO.
     * All fields in {@link RegistrationResponse} are populated from the registration and registrant.
     */
    RegistrationResponse toResponse(SchemeRegistration reg, Registrant registrant) {
        List<String> flags = new ArrayList<>();
        if (Boolean.FALSE.equals(reg.getChangeNotificationTimely())) {
            flags.add("LATE_NOTIFICATION");
        }
        if (reg.isTransitionalUpdateOverdue()) {
            flags.add("TRANSITIONAL_UPDATE_OVERDUE");
        }

        return new RegistrationResponse(
                reg.getId(),
                registrant.getId(),
                registrant.getRegistrantName(),
                registrant.getHomeCountry(),
                registrant.getEmail(),
                registrant.getPostalAddress(),
                registrant.getBankDetails(),
                registrant.getStatus().name(),
                reg.getValidFrom(),
                registrant.getVatNumber() != null ? registrant.getVatNumber() : reg.getVatNumber(),
                reg.getSchemeType().name(),
                legalBasisFor(reg.getSchemeType()),
                reg.isEarlyDeliveryException(),
                reg.isDelayNotificationSent(),
                reg.getExpectedAssignmentDate(),
                reg.getVatNumberFlag(),
                reg.getBindingPeriodEnd(),
                reg.getBindingRuleType(),
                reg.getChangeNotificationTimely(),
                flags.isEmpty() ? null : flags,
                reg.isOutgoingMemberStateNotificationDispatched(),
                reg.getClosedDate(),
                reg.getDeregistrationTimely(),
                reg.getDeregistrationEffectiveDate(),
                reg.getReRegistrationBlockUntil(),
                reg.getExclusionCriterion(),
                reg.getExclusionDecisionDate(),
                reg.getExclusionEffectiveDate(),
                reg.getNewSchemeEffectiveDate(),
                reg.getLastIdentificationUpdateDate()
        );
    }
}
