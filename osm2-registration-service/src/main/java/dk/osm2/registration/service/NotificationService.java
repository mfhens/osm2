package dk.osm2.registration.service;

import dk.osm2.registration.domain.SchemeRegistration;
import dk.osm2.registration.dto.RegistrationResponse;
import dk.osm2.registration.exception.RegistrationNotFoundException;
import dk.osm2.registration.repository.SchemeRegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Handles outgoing electronic notifications to taxable persons.
 *
 * <p>Implementation note (OQ-4): the notification delivery channel is not yet
 * specified. This service sets the relevant flags on the {@link SchemeRegistration}
 * and logs the notification intent. No external system is called until OQ-4 is resolved.
 *
 * <p>Legal basis: ML § 66b stk. 4 (delay notification); ML § 66j (exclusion notification)
 * <p>Petition: OSS-02 — FR-OSS-02-014, FR-OSS-02-033
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {

    /** Day-8 deadline: if approval is not processed within 8 days, a delay notification is sent. */
    private static final int APPROVAL_DEADLINE_DAYS = 8;

    /** Standard lead time assumed for VAT number assignment after delay notification. */
    private static final int EXPECTED_ASSIGNMENT_LEAD_DAYS = 30;

    private final SchemeRegistrationRepository schemeRegistrationRepository;
    private final RegistrationService registrationService;

    /**
     * Check whether the day-8 approval deadline has been exceeded for a pending registration
     * and, if so, send a delay notification to the taxable person.
     *
     * @param registrationId the registration to evaluate
     * @param evaluationDate the date from which to measure (prevents test date drift)
     * @return updated {@link RegistrationResponse} with delay-notification flags set
     */
    public RegistrationResponse checkAndSendDelayNotification(UUID registrationId, LocalDate evaluationDate) {
        SchemeRegistration reg = schemeRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new RegistrationNotFoundException(registrationId));

        if (reg.isDelayNotificationSent()) {
            return registrationService.toResponse(reg, reg.getRegistrant());
        }

        LocalDate submittedDate = reg.getNotificationSubmittedAt() != null
                ? reg.getNotificationSubmittedAt().toLocalDate()
                : reg.getValidFrom();

        LocalDate deadline = submittedDate.plusDays(APPROVAL_DEADLINE_DAYS);

        if (!evaluationDate.isBefore(deadline) && "PENDING_VAT_NUMBER".equals(reg.getRegistrationStatus())) {
            LocalDate expectedAssignment = evaluationDate.plusDays(EXPECTED_ASSIGNMENT_LEAD_DAYS);
            reg.setDelayNotificationSent(true);
            reg.setExpectedAssignmentDate(expectedAssignment);
            schemeRegistrationRepository.save(reg);

            log.warn("OQ-4: Delay notification dispatched for registrationId={} — approval deadline {} exceeded. "
                    + "Expected assignment date: {}", registrationId, deadline, expectedAssignment);
        }

        return registrationService.toResponse(reg, reg.getRegistrant());
    }
}
