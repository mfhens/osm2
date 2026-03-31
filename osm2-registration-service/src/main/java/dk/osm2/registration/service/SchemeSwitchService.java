package dk.osm2.registration.service;

import dk.osm2.registration.domain.*;
import dk.osm2.registration.dto.ApprovalRequest;
import dk.osm2.registration.dto.ExclusionRequest;
import dk.osm2.registration.dto.RegistrationRequest;
import dk.osm2.registration.dto.RegistrationResponse;
import dk.osm2.registration.exception.RegistrationNotFoundException;
import dk.osm2.registration.repository.SchemeRegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles atomic scheme switching when a registrant's establishment status changes.
 *
 * <p>Two transitions are supported:
 * <ul>
 *   <li>EU → Non-EU: taxable person loses last EU fixed establishment</li>
 *   <li>Non-EU → EU: taxable person gains a fixed place of business in a member state</li>
 * </ul>
 *
 * <p>In both cases the old registration is excluded and a new registration for the
 * target scheme is created with the same effective date, so there is no gap period.
 *
 * <p>Legal basis: ML § 66j stk. 1 nr. 3; OSS-02 FR-OSS-02-037
 * <p>Petition: OSS-02
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SchemeSwitchService {

    private final SchemeRegistrationRepository schemeRegistrationRepository;
    private final RegistrationService registrationService;

    /**
     * Process an establishment-change event that triggers a scheme switch.
     *
     * @param registrationId current active registration to exclude
     * @param body           request map with keys: changeDate (ISO), changeType, newScheme
     * @return updated {@link RegistrationResponse} for the old (excluded) registration,
     *         with {@link RegistrationResponse#newSchemeEffectiveDate()} set
     */
    public RegistrationResponse processEstablishmentChange(UUID registrationId, Map<String, Object> body) {
        SchemeRegistration old = schemeRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new RegistrationNotFoundException(registrationId));

        LocalDate changeDate = LocalDate.parse((String) body.get("changeDate"));
        String newScheme = (String) body.get("newScheme");

        // Exclude the old registration on the change date
        ExclusionRequest exclusionRequest = new ExclusionRequest("CONDITIONS_NOT_MET", changeDate, null);
        registrationService.excludeRegistration(registrationId, exclusionRequest);

        // Re-fetch to get updated state
        old = schemeRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new RegistrationNotFoundException(registrationId));

        // Create a new registration for the target scheme, effective the same date
        Registrant registrant = old.getRegistrant();
        RegistrationRequest switchRequest = new RegistrationRequest(
                registrant.getBankDetails(),
                registrant.getEmail(),
                registrant.getPhoneNumber(),
                registrant.getPostalAddress(),
                registrant.getHomeCountryTaxNumber(),
                registrant.getRegistrantName(),
                newScheme,
                registrant.getHomeCountry(),
                List.of(),
                null,
                registrant.getIdentificationMemberState(),
                null,
                changeDate,    // desiredStartDate = changeDate → no gap
                false,
                false,
                registrant.getVatNumber()
        );

        RegistrationResponse newReg = registrationService.submitRegistration(switchRequest);

        // Approve the new registration immediately (scheme switch = automatic approval)
        registrationService.approveRegistration(
                newReg.registrationId(),
                new ApprovalRequest(registrant.getVatNumber()));

        // Record the new scheme effective date on the old registration for the response
        old.setNewSchemeEffectiveDate(changeDate);
        schemeRegistrationRepository.save(old);

        log.info("Scheme switch: oldRegistrationId={} oldScheme={} newScheme={} effectiveDate={}",
                registrationId, old.getSchemeType(), newScheme, changeDate);

        return registrationService.toResponse(old, registrant);
    }
}
