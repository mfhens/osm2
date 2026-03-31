package dk.osm2.registration.service;

import dk.osm2.registration.domain.SchemeRegistration;
import dk.osm2.registration.dto.RegistrationResponse;
import dk.osm2.registration.exception.RegistrationNotFoundException;
import dk.osm2.registration.repository.SchemeRegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Enforces the transitional provision introduced by Directive 2017/2455.
 *
 * <p>All OSS registrations created before 1 July 2021 must be updated with
 * the new identification information required by the updated rules.
 * Registrants that have not submitted the update by 1 April 2022 are flagged
 * as {@code TRANSITIONAL_UPDATE_OVERDUE}.
 *
 * <p>Consequences of the flag:
 * <ul>
 *   <li>The flag appears in the compliance-review queue.</li>
 *   <li>Opening a new quarterly return period is blocked until the update is submitted.</li>
 * </ul>
 *
 * <p>Legal basis: Direktiv 2017/2455 art. 2 stk. 3; ML § 66b (transitional)
 * <p>Petition: OSS-02 — FR-OSS-02-038
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TransitionalComplianceService {

    /** Date from which the updated identification rules became applicable. */
    private static final LocalDate DIRECTIVE_EFFECTIVE_DATE = LocalDate.of(2021, 7, 1);

    /** Grace period end: registrations not yet updated by this date are flagged. */
    private static final LocalDate GRACE_PERIOD_END = LocalDate.of(2022, 4, 1);

    private final SchemeRegistrationRepository schemeRegistrationRepository;
    private final RegistrationService registrationService;

    /**
     * Evaluate whether a registration is overdue for a transitional identification update.
     *
     * <p>Sets the {@code transitionalUpdateOverdue} flag when:
     * <ol>
     *   <li>The registration's {@code validFrom} is before {@link #DIRECTIVE_EFFECTIVE_DATE}.</li>
     *   <li>No identification update has been submitted ({@code lastIdentificationUpdateDate} is null).</li>
     *   <li>{@code systemDate} is on or after {@link #GRACE_PERIOD_END}.</li>
     * </ol>
     *
     * @param registrationId the registration to evaluate
     * @param body           map with key {@code systemDate} (ISO date string)
     * @return updated {@link RegistrationResponse}
     */
    public RegistrationResponse evaluateTransitional(UUID registrationId, Map<String, Object> body) {
        SchemeRegistration reg = findRegistration(registrationId);

        LocalDate systemDate = LocalDate.parse((String) body.get("systemDate"));

        boolean isPreDirective = reg.getValidFrom().isBefore(DIRECTIVE_EFFECTIVE_DATE);
        boolean noUpdateSubmitted = reg.getLastIdentificationUpdateDate() == null;
        boolean gracePeriodExpired = !systemDate.isBefore(GRACE_PERIOD_END);

        if (isPreDirective && noUpdateSubmitted && gracePeriodExpired) {
            reg.setTransitionalUpdateOverdue(true);
            schemeRegistrationRepository.save(reg);
            log.info("Transitional flag set: registrationId={} systemDate={}", registrationId, systemDate);
        } else {
            log.debug("Transitional flag NOT set: registrationId={} isPreDirective={} noUpdate={} gracePeriodExpired={}",
                    registrationId, isPreDirective, noUpdateSubmitted, gracePeriodExpired);
        }

        return registrationService.toResponse(reg, reg.getRegistrant());
    }

    /**
     * Accept a transitional identification update, clear the overdue flag, and record the update date.
     *
     * @param registrationId the registration being updated
     * @param body           map containing update fields (name, address, email, etc.)
     * @return updated {@link RegistrationResponse} with flag cleared and {@code lastIdentificationUpdateDate} set
     */
    public RegistrationResponse submitIdentificationUpdate(UUID registrationId, Map<String, Object> body) {
        SchemeRegistration reg = findRegistration(registrationId);

        reg.setTransitionalUpdateOverdue(false);
        reg.setLastIdentificationUpdateDate(LocalDate.now());
        schemeRegistrationRepository.save(reg);

        log.info("Transitional identification update submitted: registrationId={}", registrationId);

        return registrationService.toResponse(reg, reg.getRegistrant());
    }

    /**
     * Attempt to open a new quarterly return period.
     *
     * <p>Blocked if the registration is flagged as {@code TRANSITIONAL_UPDATE_OVERDUE}.
     *
     * @throws ResponseStatusException 400 Bad Request if the flag is set
     */
    public void openReturnPeriod(UUID registrationId) {
        SchemeRegistration reg = findRegistration(registrationId);

        if (reg.isTransitionalUpdateOverdue()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "TRANSITIONAL_UPDATE_REQUIRED: Cannot open return period until identification update is submitted.");
        }

        log.info("Return period opening allowed: registrationId={}", registrationId);
    }

    private SchemeRegistration findRegistration(UUID registrationId) {
        return schemeRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new RegistrationNotFoundException(registrationId));
    }
}
