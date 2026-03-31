package dk.osm2.registration.service;

import dk.osm2.registration.domain.*;
import dk.osm2.registration.dto.ExclusionRequest;
import dk.osm2.registration.exception.RegistrationNotFoundException;
import dk.osm2.registration.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service for exclusion-related operations.
 *
 * <p>Handles recording of exclusions and querying active exclusion bans.
 *
 * <p>Legal basis: ML § 66j stk. 1–2; ML § 66d stk. 3
 * <p>Petition: OSS-02 — FR-OSS-02-031 through FR-OSS-02-035
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExclusionService {

    private final SchemeRegistrationRepository schemeRegistrationRepository;
    private final ExclusionBanRepository exclusionBanRepository;
    private final RegistrantRepository registrantRepository;

    /**
     * Record an exclusion for a registration.
     *
     * <p>Sets valid_to, updates status to EXCLUDED.
     * Creates an ExclusionBan if criterion is PERSISTENT_NON_COMPLIANCE.
     * banLiftedAt = effectiveDate + 2 years (per ML § 66j stk. 2).
     *
     * @param registrationId the scheme_registration ID
     * @param request        the exclusion details
     */
    public void recordExclusion(UUID registrationId, ExclusionRequest request) {
        SchemeRegistration reg = schemeRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new RegistrationNotFoundException(registrationId));
        Registrant registrant = reg.getRegistrant();

        reg.setValidTo(request.effectiveDate());
        reg.setExclusionCriterion(request.criterion());
        reg.setExclusionDecisionDate(LocalDate.now());
        reg.setExclusionEffectiveDate(request.effectiveDate());
        schemeRegistrationRepository.save(reg);

        registrant.setStatus(RegistrantStatus.EXCLUDED);
        registrantRepository.save(registrant);

        // Create 2-year re-registration ban for PERSISTENT_NON_COMPLIANCE
        if ("PERSISTENT_NON_COMPLIANCE".equals(request.criterion())) {
            ExclusionBan ban = new ExclusionBan();
            ban.setRegistrant(registrant);
            ban.setSchemeType(reg.getSchemeType());
            ban.setExclusionRegistration(reg);
            ban.setCriterion(request.criterion());
            ban.setBanLiftedAt(request.effectiveDate().plusYears(2));
            exclusionBanRepository.save(ban);

            reg.setReRegistrationBlockUntil(request.effectiveDate().plusYears(2));
            schemeRegistrationRepository.save(reg);

            log.info("2-year exclusion ban created: registrantId={} banLiftedAt={}",
                    registrant.getId(), request.effectiveDate().plusYears(2));
        }
    }

    /**
     * Check whether an active exclusion ban exists for a registrant in a given scheme.
     *
     * @param registrantId the registrant's UUID
     * @param schemeType   the OSS scheme type
     * @return true if a ban is active today
     */
    @Transactional(readOnly = true)
    public boolean isExclusionBanActive(UUID registrantId, SchemeType schemeType) {
        List<ExclusionBan> bans = exclusionBanRepository.findActiveBans(
                registrantId, schemeType, LocalDate.now());
        return !bans.isEmpty();
    }
}
