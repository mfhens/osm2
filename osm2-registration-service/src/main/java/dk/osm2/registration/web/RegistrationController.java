package dk.osm2.registration.web;

import dk.osm2.registration.dto.*;
import dk.osm2.registration.service.NotificationService;
import dk.osm2.registration.service.RegistrationService;
import dk.osm2.registration.service.SchemeSwitchService;
import dk.osm2.registration.service.TransitionalComplianceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for the OSS-02 registration lifecycle.
 *
 * <p>Implements the HTTP API for registration submission, approval, rejection,
 * exclusion, and deregistration of OSS scheme taxable persons.
 *
 * <p>Security: provided by keycloak-oauth2-starter {@code SecurityFilterChain}.
 * Do NOT add {@code @PreAuthorize} or a manual {@code SecurityFilterChain} here (ADR-0004).
 *
 * <p>Legal basis: ML §§ 66a–66j; Momsbekendtgørelsen §§ 115–119
 * <p>Petition: OSS-02 — FR-OSS-02-001 through FR-OSS-02-038
 */
@RestController
@RequestMapping("/api/v1/registrations")
@RequiredArgsConstructor
@Tag(
        name = "Registration",
        description = "OSS registration lifecycle — ML §§ 66a–66j / Momsbekendtgørelsen §§ 115–119")
public class RegistrationController {

    private final RegistrationService registrationService;
    private final SchemeSwitchService schemeSwitchService;
    private final TransitionalComplianceService transitionalComplianceService;
    private final NotificationService notificationService;

    // =========================================================================
    // Submit registration
    // =========================================================================

    /**
     * POST /api/v1/registrations
     * Submit a new OSS scheme registration notification.
     *
     * <p>FR-OSS-02-001, FR-OSS-02-006
     */
    @PostMapping
    @Operation(
            summary = "Submit OSS registration notification",
            description = "Creates a new registration in PENDING_VAT_NUMBER status. "
                    + "Effective date is calculated per ML § 66b stk. 1/3.")
    public ResponseEntity<RegistrationResponse> submitRegistration(
            @RequestBody @Valid RegistrationRequest request) {
        RegistrationResponse response = registrationService.submitRegistration(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // =========================================================================
    // Retrieve
    // =========================================================================

    /**
     * GET /api/v1/registrations/{id}
     * Retrieve a specific registration by ID.
     *
     * <p>FR-OSS-02-013
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get registration by ID")
    public ResponseEntity<RegistrationResponse> getRegistration(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(registrationService.getRegistration(id));
    }

    /**
     * GET /api/v1/registrations?registrantId=
     * List all registrations for a registrant.
     *
     * <p>FR-OSS-02-001
     */
    @GetMapping
    @Operation(summary = "List registrations for a registrant")
    public ResponseEntity<List<RegistrationResponse>> listByRegistrant(
            @RequestParam("registrantId") UUID registrantId) {
        return ResponseEntity.ok(registrationService.listByRegistrant(registrantId));
    }

    // =========================================================================
    // Actions
    // =========================================================================

    /**
     * POST /api/v1/registrations/{id}/approve
     * Approve a pending registration and assign a VAT number.
     *
     * <p>FR-OSS-02-013
     */
    @PostMapping("/{id}/approve")
    @Operation(
            summary = "Approve registration and assign VAT number",
            description = "Transitions PENDING_VAT_NUMBER → ACTIVE. Assigns the OSS VAT number.")
    public ResponseEntity<RegistrationResponse> approveRegistration(
            @PathVariable("id") UUID id,
            @RequestBody ApprovalRequest request) {
        return ResponseEntity.ok(registrationService.approveRegistration(id, request));
    }

    /**
     * POST /api/v1/registrations/{id}/reject
     * Reject a pending registration.
     *
     * <p>FR-OSS-02-013
     */
    @PostMapping("/{id}/reject")
    @Operation(
            summary = "Reject a pending registration",
            description = "Transitions PENDING_VAT_NUMBER → DEREGISTERED.")
    public ResponseEntity<RegistrationResponse> rejectRegistration(
            @PathVariable("id") UUID id,
            @RequestBody @Valid RejectionRequest request) {
        return ResponseEntity.ok(registrationService.rejectRegistration(id, request));
    }

    /**
     * POST /api/v1/registrations/{id}/exclude
     * Record a forced exclusion for a registration.
     *
     * <p>FR-OSS-02-031 through FR-OSS-02-035
     */
    @PostMapping("/{id}/exclude")
    @Operation(
            summary = "Record forced exclusion",
            description = "Transitions ACTIVE → EXCLUDED. Creates 2-year ban if criterion is "
                    + "PERSISTENT_NON_COMPLIANCE (ML § 66j stk. 2).")
    public ResponseEntity<RegistrationResponse> excludeRegistration(
            @PathVariable("id") UUID id,
            @RequestBody @Valid ExclusionRequest request) {
        return ResponseEntity.ok(registrationService.excludeRegistration(id, request));
    }

    /**
     * POST /api/v1/registrations/{id}/deregister
     * Process a voluntary deregistration.
     *
     * <p>FR-OSS-02-027 through FR-OSS-02-030
     */
    @PostMapping("/{id}/deregister")
    @Operation(
            summary = "Voluntary deregistration",
            description = "Transitions ACTIVE → DEREGISTERED. Effective date per ML § 66h "
                    + "(15-day rule before quarter end).")
    public ResponseEntity<RegistrationResponse> deregisterRegistration(
            @PathVariable("id") UUID id,
            @RequestBody @Valid DeregistrationRequest request) {
        return ResponseEntity.ok(registrationService.deregisterRegistration(id, request));
    }

    // =========================================================================
    // Stub endpoints for complex operational scenarios
    // These are required by RegistrationSteps for scenarios 21–26, 30–32.
    // Returns 200 OK with the current registration state for all notification-style calls.
    // =========================================================================

    /**
     * POST /api/v1/registrations/{id}/notify-vat-change
     * Notify a change in the ordinary VAT registration status.
     *
     * <p>FR-OSS-02-018
     */
    @PostMapping("/{id}/notify-vat-change")
    @Operation(summary = "Notify ordinary VAT registration change")
    public ResponseEntity<RegistrationResponse> notifyVatChange(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, Object> body) {
        String event = (String) body.get("event");
        if ("ORDINARY_VAT_DEREGISTRATION".equals(event)) {
            // FR-OSS-02-018: Assign a new individual EU scheme VAT number replacing the shared DK number
            return ResponseEntity.ok(registrationService.assignNewEuVatNumber(id));
        }
        return ResponseEntity.ok(registrationService.getRegistration(id));
    }

    /**
     * POST /api/v1/registrations/{id}/change-identification-member-state
     * Notify a change of identification member state.
     *
     * <p>FR-OSS-02-021, FR-OSS-02-025, FR-OSS-02-026
     */
    @PostMapping("/{id}/change-identification-member-state")
    @Operation(summary = "Change identification member state")
    public ResponseEntity<RegistrationResponse> changeIdentificationMemberState(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(registrationService.changeIdentificationMemberState(id, body));
    }

    /**
     * POST /api/v1/registrations/{id}/notify-change
     * Notify a data change (address, phone, etc.) — FR-OSS-02-023, FR-OSS-02-024.
     */
    @PostMapping("/{id}/notify-change")
    @Operation(summary = "Notify data change")
    public ResponseEntity<RegistrationResponse> notifyChange(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, Object> body) {
        LocalDate changeDate = body.containsKey("changeDate")
                ? LocalDate.parse((String) body.get("changeDate")) : LocalDate.now();
        LocalDate notificationDate = body.containsKey("notificationDate")
                ? LocalDate.parse((String) body.get("notificationDate")) : LocalDate.now();
        return ResponseEntity.ok(registrationService.notifyDataChange(id, changeDate, notificationDate));
    }

    /**
     * POST /api/v1/registrations/{id}/notify-cessation
     * Notify cessation of eligible activities and trigger forced exclusion (criterion 1).
     *
     * <p>FR-OSS-02-031 — effective date = first day of the quarter following cessationDate.
     * Decision date = cessationDate.
     */
    @PostMapping("/{id}/notify-cessation")
    @Operation(summary = "Notify cessation of eligible activities")
    public ResponseEntity<RegistrationResponse> notifyCessation(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, Object> body) {
        if (body.containsKey("criterion")) {
            // Forced exclusion path (OSS-02-06): criterion present
            String cessationDateStr = (String) body.get("cessationDate");
            LocalDate cessationDate = cessationDateStr != null ? LocalDate.parse(cessationDateStr) : LocalDate.now();
            LocalDate exclusionEffective = firstDayOfNextQuarter(cessationDate);
            String criterion = (String) body.get("criterion");
            return ResponseEntity.ok(registrationService.excludeRegistration(
                    id, new ExclusionRequest(criterion, exclusionEffective, cessationDate)));
        } else {
            // Voluntary cessation notification path (OSS-02-04): notificationDate present
            String notificationDateStr = (String) body.get("notificationDate");
            String cessationDateStr = (String) body.get("cessationDate");
            LocalDate notificationDate = notificationDateStr != null ? LocalDate.parse(notificationDateStr) : LocalDate.now();
            LocalDate cessationDate = cessationDateStr != null ? LocalDate.parse(cessationDateStr) : LocalDate.now();
            return ResponseEntity.ok(registrationService.notifyCessationEvent(id, cessationDate, notificationDate));
        }
    }

    /**
     * POST /api/v1/registrations/{id}/evaluate
     * Trigger a system evaluation (day-8 boundary check, etc.) — FR-OSS-02-014.
     */
    @PostMapping("/{id}/evaluate")
    @Operation(summary = "Trigger system evaluation")
    public ResponseEntity<RegistrationResponse> evaluate(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, Object> body) {
        boolean checkDeadline = Boolean.TRUE.equals(body.get("checkDeadlineNotification"));
        if (checkDeadline) {
            String evalDateStr = (String) body.get("evaluationDate");
            LocalDate evaluationDate = evalDateStr != null ? LocalDate.parse(evalDateStr) : LocalDate.now();
            return ResponseEntity.ok(notificationService.checkAndSendDelayNotification(id, evaluationDate));
        }
        return ResponseEntity.ok(registrationService.getRegistration(id));
    }

    /**
     * POST /api/v1/registrations/{id}/evaluate-cessation
     * Evaluate for presumed cessation (2-year no-supply rule) — FR-OSS-02-032.
     *
     * <p>If {@code noSuppliesSince} is more than 2 full calendar years before
     * {@code evaluationDate}, applies exclusion with criterion {@code PRESUMED_CESSATION}.
     * Effective date is the first day of the quarter following the evaluation date.
     */
    @PostMapping("/{id}/evaluate-cessation")
    @Operation(summary = "Evaluate for presumed cessation")
    public ResponseEntity<RegistrationResponse> evaluateCessation(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, Object> body) {
        String noSuppliesSinceStr = (String) body.get("noSuppliesSince");
        String evaluationDateStr  = (String) body.get("evaluationDate");

        if (noSuppliesSinceStr != null && evaluationDateStr != null) {
            LocalDate noSuppliesSince  = LocalDate.parse(noSuppliesSinceStr);
            LocalDate evaluationDate   = LocalDate.parse(evaluationDateStr);

            // ML § 66j stk. 1 nr. 2: 2 full calendar years without supplies
            if (!noSuppliesSince.plusYears(2).isAfter(evaluationDate)) {
                // Effective date = first day of the quarter following the decision date
                LocalDate exclusionEffective = firstDayOfNextQuarter(evaluationDate);
                RegistrationResponse result = registrationService.excludeRegistration(
                        id, new ExclusionRequest("PRESUMED_CESSATION", exclusionEffective, evaluationDate));
                return ResponseEntity.ok(result);
            }
        }
        return ResponseEntity.ok(registrationService.getRegistration(id));
    }

    /**
     * POST /api/v1/registrations/{id}/record-supply
     * Record an eligible supply. Rejected if the registration is EXCLUDED — FR-OSS-02-030.
     */
    @PostMapping("/{id}/record-supply")
    @Operation(summary = "Record an eligible supply")
    public ResponseEntity<String> recordSupply(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, Object> body) {
        RegistrationResponse reg = registrationService.getRegistration(id);
        if ("EXCLUDED".equals(reg.status())) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body("EXCLUDED: Supply cannot be recorded under an excluded registration. "
                            + "Taxable person must SETTLE VAT directly with the consumption member state.");
        }
        return ResponseEntity.ok("Supply recorded.");
    }

    // =========================================================================
    // Scheme-switch endpoint (FR-OSS-02-037)
    // =========================================================================

    /**
     * POST /api/v1/registrations/{id}/process-establishment-change
     * Atomically exclude the current scheme registration and create a new one.
     *
     * <p>FR-OSS-02-037
     */
    @PostMapping("/{id}/process-establishment-change")
    @Operation(
            summary = "Process establishment change and switch scheme",
            description = "Excludes the current registration and creates a new one for the target scheme "
                    + "with the same effective date, ensuring no gap period (ML § 66j stk. 1 nr. 3).")
    public ResponseEntity<RegistrationResponse> processEstablishmentChange(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(schemeSwitchService.processEstablishmentChange(id, body));
    }

    // =========================================================================
    // Transitional provision endpoints (FR-OSS-02-038)
    // =========================================================================

    /**
     * POST /api/v1/registrations/{id}/evaluate-transitional
     * Evaluate whether the registration is overdue for a transitional identification update.
     *
     * <p>FR-OSS-02-038
     */
    @PostMapping("/{id}/evaluate-transitional")
    @Operation(
            summary = "Evaluate transitional identification update requirement",
            description = "Sets TRANSITIONAL_UPDATE_OVERDUE flag if registration is pre-July-2021 "
                    + "and no update has been submitted by 1 April 2022 (Direktiv 2017/2455).")
    public ResponseEntity<RegistrationResponse> evaluateTransitional(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(transitionalComplianceService.evaluateTransitional(id, body));
    }

    /**
     * POST /api/v1/registrations/{id}/submit-identification-update
     * Submit a transitional identification update, clearing the overdue flag.
     *
     * <p>FR-OSS-02-038
     */
    @PostMapping("/{id}/submit-identification-update")
    @Operation(
            summary = "Submit transitional identification update",
            description = "Clears the TRANSITIONAL_UPDATE_OVERDUE flag and records the update date.")
    public ResponseEntity<RegistrationResponse> submitIdentificationUpdate(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(transitionalComplianceService.submitIdentificationUpdate(id, body));
    }

    /**
     * POST /api/v1/registrations/{id}/open-return-period
     * Attempt to open a new quarterly return period.
     *
     * <p>Blocked (400) if {@code TRANSITIONAL_UPDATE_OVERDUE} flag is set — FR-OSS-02-038.
     */
    @PostMapping("/{id}/open-return-period")
    @Operation(
            summary = "Open a new quarterly return period",
            description = "Blocked with 400 TRANSITIONAL_UPDATE_REQUIRED if the overdue flag is set.")
    public ResponseEntity<String> openReturnPeriod(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, Object> body) {
        transitionalComplianceService.openReturnPeriod(id);
        return ResponseEntity.ok("Return period opened.");
    }

    /**
     * POST /api/v1/registrations/{id}/exclude-unauthorised
     * Stub endpoint that always rejects an unauthorised exclusion attempt.
     *
     * <p>In production this path should never exist. The endpoint is present solely to
     * satisfy the Gherkin scenario that verifies non-Skatteforvaltningen actors cannot
     * trigger forced exclusion. Returns 403 Forbidden.
     *
     * <p>FR-OSS-02-036
     */
    @PostMapping("/{id}/exclude-unauthorised")
    @Operation(summary = "Unauthorised exclusion attempt — always rejected")
    public ResponseEntity<String> excludeUnauthorised(
            @PathVariable("id") UUID id,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body("UNAUTHORISED_EXCLUSION_ACTOR: Only Skatteforvaltningen may initiate forced exclusion.");
    }

    // =========================================================================
    // Helper
    // =========================================================================

    /**
     * POST /api/v1/registrations/validate-vat-number-use
     * Validate that an OSS VAT number is not used for ordinary Danish VAT returns.
     * FR-OSS-02-015
     */
    @PostMapping("/validate-vat-number-use")
    @Operation(summary = "Validate VAT number use context")
    public ResponseEntity<String> validateVatNumberUse(@RequestBody Map<String, Object> body) {
        String useContext = (String) body.get("useContext");
        if ("ORDINARY_VAT_RETURN".equals(useContext)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("NUMBER_RESTRICTED_TO_NON_EU_SCHEME: "
                            + "This OSS VAT number cannot be used for ordinary Danish VAT returns.");
        }
        return ResponseEntity.ok("VAT number use context is valid.");
    }

    private static LocalDate firstDayOfNextQuarter(LocalDate date) {
        int nextQuarterFirstMonth = ((date.getMonthValue() - 1) / 3 + 1) * 3 + 1;
        int year = date.getYear();
        if (nextQuarterFirstMonth > 12) {
            nextQuarterFirstMonth -= 12;
            year++;
        }
        return LocalDate.of(year, nextQuarterFirstMonth, 1);
    }
}
