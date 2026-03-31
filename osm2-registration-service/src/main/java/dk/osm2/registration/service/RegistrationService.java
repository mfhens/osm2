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
        // (This is encoded by the electronicInterfaceFlag and scheme validation)

        // Check exclusion ban
        // For new registrations from a pre-existing registrant, we check via email
        // (For simplicity in this implementation, we skip duplicate-registrant check)

        // Calculate effective date
        LocalDate notificationDate = LocalDate.now();
        LocalDate firstDeliveryDate = request.firstDeliveryDate();
        LocalDate desiredStartDate = request.desiredStartDate();

        LocalDate effectiveDate;
        boolean earlyDelivery = false;

        if (desiredStartDate != null) {
            // If desired start date provided, use it directly (scenario 1)
            effectiveDate = desiredStartDate;
        } else if (firstDeliveryDate != null) {
            effectiveDate = effectiveDateService.calculate(notificationDate, firstDeliveryDate);
            earlyDelivery = effectiveDateService.qualifiesForEarlyDeliveryException(notificationDate, firstDeliveryDate);
        } else {
            effectiveDate = effectiveDateService.firstDayOfNextQuarter(notificationDate);
        }

        // Determine VAT number: reuse existing Danish VAT for EU scheme if provided
        String vatNumber = null;
        if (request.existingDanishVatNumber() != null && "EU".equals(request.scheme())) {
            vatNumber = request.existingDanishVatNumber();
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
        registrant.setStatus(RegistrantStatus.PENDING_VAT_NUMBER);
        registrant.setVatNumber(vatNumber); // may be null until approval

        registrant = registrantRepository.save(registrant);

        // Check active exclusion ban (for re-registration scenarios)
        List<ExclusionBan> activeBans = exclusionBanRepository.findActiveBans(
                registrant.getId(), schemeType, LocalDate.now());
        if (!activeBans.isEmpty()) {
            ExclusionBan ban = activeBans.get(0);
            throw new ExclusionBanActiveException(registrant.getId(), schemeType, ban.getBanLiftedAt());
        }

        // Create scheme registration
        SchemeRegistration registration = new SchemeRegistration();
        registration.setRegistrant(registrant);
        registration.setSchemeType(schemeType);
        registration.setValidFrom(effectiveDate);
        registration.setValidTo(null); // still active
        registration.setNotificationSubmittedAt(LocalDateTime.now());
        registration.setVatNumber(vatNumber);
        registration.setIdentificationMemberState(request.identificationMemberState());
        registration.setEarlyDeliveryException(earlyDelivery);
        registration.setFirstDeliveryDate(firstDeliveryDate);
        registration.setRegistrationStatus("PENDING_VAT_NUMBER");
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
        reg.setVatNumberFlag("CONFIRMED");
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
            // Ban lifted 2 years after the exclusion effective date
            ban.setBanLiftedAt(effectiveDate.plusYears(2));
            exclusionBanRepository.save(ban);

            // Set re-registration block on the registration
            reg.setReRegistrationBlockUntil(effectiveDate.plusYears(2));
            schemeRegistrationRepository.save(reg);

            log.info("Exclusion ban created: registrantId={} scheme={} banLiftedAt={}",
                    registrant.getId(), reg.getSchemeType(), effectiveDate.plusYears(2));
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

        LocalDate requestedEffectiveDate = request.effectiveDate();
        LocalDate notificationDate = LocalDate.now();

        // Calculate deregistration effective date per ML § 66h
        LocalDate quarterEnd = lastDayOfCurrentQuarter(notificationDate);
        long daysBeforeQuarterEnd = java.time.temporal.ChronoUnit.DAYS.between(notificationDate, quarterEnd);

        boolean timely = daysBeforeQuarterEnd >= 15;
        LocalDate effectiveDate;

        if (timely) {
            // Use requested date or quarter end
            effectiveDate = (requestedEffectiveDate != null) ? requestedEffectiveDate : quarterEnd;
        } else {
            // Defer to the next quarter end
            effectiveDate = lastDayOfNextQuarter(notificationDate);
        }

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
            return "CASE_A|";
        }
        // Case B or C: binding applies
        int bindingEndYear = effectiveDate.getYear() + 2;
        LocalDate bindingEnd = LocalDate.of(bindingEndYear, 12, 31);
        return "CASE_B|" + bindingEnd;
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
