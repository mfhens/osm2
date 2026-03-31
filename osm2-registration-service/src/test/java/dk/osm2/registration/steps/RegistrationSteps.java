package dk.osm2.registration.steps;

// ─── Standard Java ────────────────────────────────────────────────────────────
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// ─── Spring / TestRestTemplate ────────────────────────────────────────────────
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

// ─── Cucumber ─────────────────────────────────────────────────────────────────
import io.cucumber.java.Before;
import io.cucumber.java.DataTableType;
import io.cucumber.java.ParameterType;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.datatable.DataTable;

// ─── AssertJ ──────────────────────────────────────────────────────────────────
import static org.assertj.core.api.Assertions.assertThat;

// ─────────────────────────────────────────────────────────────────────────────
// INTENTIONAL COMPILE-TIME FAILURES
//
// The six imports below reference Java `record` types that do NOT YET EXIST in
// dk.osm2.registration.dto.  This file WILL NOT COMPILE until the production
// implementation creates those types.  That compile failure is the RED state
// required by BDD test-first discipline (OSS-02 petition, SDLC gate: BDD).
//
// Petition: OSS-02
// Legal basis: ML §§ 66a–66j; Momsbekendtgørelsen §§ 115–119
// FR refs: FR-OSS-02-001 through FR-OSS-02-038 (49 scenarios)
// ─────────────────────────────────────────────────────────────────────────────
import dk.osm2.registration.dto.ApprovalRequest;
import dk.osm2.registration.dto.DeregistrationRequest;
import dk.osm2.registration.dto.ExclusionRequest;
import dk.osm2.registration.dto.RegistrationRequest;
import dk.osm2.registration.dto.RegistrationResponse;
import dk.osm2.registration.dto.RejectionRequest;

/**
 * Cucumber step definitions for OSS-02 — Registrering og afmeldelse.
 *
 * <p>ALL step definitions in this file are INTENTIONALLY FAILING until the
 * following production artefacts are created:
 * <ul>
 *   <li>{@code dk.osm2.registration.dto.RegistrationRequest}  (Java record)</li>
 *   <li>{@code dk.osm2.registration.dto.RegistrationResponse} (Java record)</li>
 *   <li>{@code dk.osm2.registration.dto.ApprovalRequest}      (Java record)</li>
 *   <li>{@code dk.osm2.registration.dto.RejectionRequest}     (Java record)</li>
 *   <li>{@code dk.osm2.registration.dto.ExclusionRequest}     (Java record)</li>
 *   <li>{@code dk.osm2.registration.dto.DeregistrationRequest}(Java record)</li>
 *   <li>REST controller at {@code /api/v1/registrations}</li>
 * </ul>
 *
 * <p><b>Failure mode:</b>
 * <ol>
 *   <li>Compile-time — missing DTO imports above prevent the class from compiling.</li>
 *   <li>Runtime (once DTOs exist) — HTTP calls to non-existent endpoints return
 *       4xx/5xx and AssertJ assertions throw {@link AssertionError}.</li>
 * </ol>
 *
 * <p>Every Gherkin scenario in {@code OSS-02.feature} (49 total) is covered.
 * Step-to-FR mapping is documented inline.
 *
 * <p>Open-question assumptions (per prompt spec):
 * <ul>
 *   <li>OQ-1 VAT format: {@code "DK" + 8-char UUID segment} as placeholder.</li>
 *   <li>OQ-2 Status enum: uses {@code PENDING_VAT_NUMBER}, not {@code PENDING}.</li>
 *   <li>OQ-3 15-day window: inclusive count (≤ 15 days counts as timely).</li>
 *   <li>OQ-4 Notification: assert HTTP 200; no notification-delivery assertion.</li>
 *   <li>OQ-5 Scheme switch: assert final state via GET after POST sequence.</li>
 * </ul>
 */
@SuppressWarnings({"SpringJavaInjectionPointsAutowiringInspection", "unused"})
public class RegistrationSteps {

    // =========================================================================
    // Infrastructure
    // =========================================================================

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    // =========================================================================
    // Scenario world state — reset before every scenario by @Before
    // =========================================================================

    // ── Registration-request fields ──────────────────────────────────────────
    private String registrantName;
    private String scheme;                  // "NON_EU" | "EU"
    private String homeCountry;
    private String homeCountryTaxNumber;
    private String postalAddress;
    private String email;
    private String phoneNumber;
    private String bankDetails;
    private List<String> tradingNames;
    private String bankAccountCountry;
    private String identificationMemberState;
    private LocalDate firstDeliveryDate;    // null → no early-delivery exception
    private LocalDate desiredStartDate;
    private String missingField;            // field deliberately omitted
    private boolean electronicInterfaceFlag;
    private boolean notEstablishedInEuDeclaration;

    // ── Applicant eligibility flags ──────────────────────────────────────────
    private boolean hasEuFixedEstablishment;
    private String euFixedEstablishmentCountry;

    // ── Pre-existing registration context ───────────────────────────────────
    private boolean hasDanishVatRegistration;
    private String existingDanishVatNumber;
    private LocalDate receiptDate;
    private LocalDate notificationDate;
    private String vatNumberToUse;

    // ── Binding-rule context ─────────────────────────────────────────────────
    private int registrationCalendarYear;
    private String bindingMemberStateCase;  // "a" | "b" | "c"
    private LocalDate bindingPeriodEndDate;

    // ── Change-notification context ──────────────────────────────────────────
    private String fieldToUpdate;
    private String newFieldValue;
    private LocalDate fieldChangeDate;

    // ── Cessation / deregistration context ──────────────────────────────────
    private LocalDate cessationDate;
    private LocalDate homeEstablishmentMoveDate;
    private String newIdentificationMemberState;
    private LocalDate deregistrationNotificationDate;

    // ── Exclusion context ────────────────────────────────────────────────────
    private String exclusionCriterion;
    private LocalDate exclusionDecisionDate;
    private LocalDate noSuppliesSince;
    private LocalDate exclusionEffectiveDate;

    // ── Scheme-switch context ────────────────────────────────────────────────
    private LocalDate establishmentChangeDate;
    private boolean qualifiesForNonEu;
    private LocalDate newEstablishmentDate;

    // ── Transitional context ─────────────────────────────────────────────────
    private LocalDate preJulyRegistrationDate;
    private LocalDate systemCurrentDate;

    // ── Supply / re-registration context ─────────────────────────────────────
    private LocalDate supplyDate;
    private String supplyCountry;
    private LocalDate reRegistrationDate;
    private boolean unauthorisedActor;
    private boolean simultaneousDeregistration;

    // ── Response state ───────────────────────────────────────────────────────
    /** Response from POST /api/v1/registrations or GET /api/v1/registrations/{id}. */
    private ResponseEntity<RegistrationResponse> lastRegistrationResponse;
    /** Response from action endpoints (approve / reject / exclude / deregister). */
    private ResponseEntity<RegistrationResponse> lastActionResponse;
    /** HTTP status from the most recent call (either above). */
    private int lastHttpStatus;
    /** Body text for non-2xx responses (error/validation). */
    private String lastErrorBody;
    /** Registration ID retained across steps within a scenario. */
    private UUID currentRegistrationId;
    /** Second registration ID (for scheme-switch or re-registration scenarios). */
    private UUID secondRegistrationId;

    // =========================================================================
    // Lifecycle
    // =========================================================================

    /** Reset all mutable world state to safe defaults before every scenario. */
    @Before
    public void resetWorld() {
        registrantName             = "Acme Services Ltd";
        scheme                     = "NON_EU";
        homeCountry                = "US";
        homeCountryTaxNumber       = "EIN-12-3456789";
        postalAddress              = "123 Main St, New York, USA";
        email                      = "vat@acme.com";
        phoneNumber                = "+1-555-0100";
        bankDetails                = "IBAN US12 3456 7890 1234";
        tradingNames               = new ArrayList<>(List.of("Acme Digital"));
        bankAccountCountry         = "US";
        identificationMemberState  = "DK";
        firstDeliveryDate          = null;
        desiredStartDate           = null;
        missingField               = null;
        electronicInterfaceFlag    = false;
        notEstablishedInEuDeclaration = false;

        hasEuFixedEstablishment    = false;
        euFixedEstablishmentCountry = null;

        hasDanishVatRegistration   = false;
        existingDanishVatNumber    = null;
        receiptDate                = null;
        notificationDate           = null;
        vatNumberToUse             = null;

        registrationCalendarYear   = 0;
        bindingMemberStateCase     = null;
        bindingPeriodEndDate       = null;

        fieldToUpdate              = null;
        newFieldValue              = null;
        fieldChangeDate            = null;

        cessationDate              = null;
        homeEstablishmentMoveDate  = null;
        newIdentificationMemberState = null;
        deregistrationNotificationDate = null;

        exclusionCriterion         = null;
        exclusionDecisionDate      = null;
        noSuppliesSince            = null;
        exclusionEffectiveDate     = null;

        establishmentChangeDate    = null;
        qualifiesForNonEu          = false;
        newEstablishmentDate       = null;

        preJulyRegistrationDate    = null;
        systemCurrentDate          = null;

        supplyDate                 = null;
        supplyCountry              = null;
        reRegistrationDate         = null;
        unauthorisedActor          = false;
        simultaneousDeregistration = false;

        lastRegistrationResponse   = null;
        lastActionResponse         = null;
        lastHttpStatus             = 0;
        lastErrorBody              = null;
        currentRegistrationId      = null;
        secondRegistrationId       = null;
    }

    // =========================================================================
    // Custom Cucumber parameter types
    // =========================================================================

    /**
     * Parses ISO-8601 date literals embedded in Gherkin step text.
     * Example: "on 2024-02-15" → the token "2024-02-15" is passed here.
     */
    @ParameterType("\\d{4}-\\d{2}-\\d{2}")
    public LocalDate localdate(String value) {
        return LocalDate.parse(value);
    }

    // =========================================================================
    // URL helpers
    // =========================================================================

    private String registrationsUrl() {
        return "http://localhost:" + port + "/api/v1/registrations";
    }

    private String registrationUrl(UUID id) {
        return "http://localhost:" + port + "/api/v1/registrations/" + id;
    }

    private String actionUrl(UUID id, String action) {
        return "http://localhost:" + port + "/api/v1/registrations/" + id + "/" + action;
    }

    // =========================================================================
    // Request-builder helpers
    // =========================================================================

    /**
     * Constructs a {@link RegistrationRequest} from current world state.
     * If {@link #missingField} is set, that field is replaced with {@code null}
     * to simulate the missing-mandatory-field scenarios (FR-OSS-02-009 / -010).
     *
     * COMPILE-TIME FAILURE: RegistrationRequest record does not yet exist.
     */
    private RegistrationRequest buildRequest() {
        return new RegistrationRequest(
                "bank_details".equals(missingField) ? null : bankDetails,
                "email".equals(missingField)        ? null : email,
                "phone".equals(missingField)        ? null : phoneNumber,
                "postal_address".equals(missingField) ? null : postalAddress,
                "home_country_tax_number".equals(missingField) ? null : homeCountryTaxNumber,
                registrantName,
                scheme,
                homeCountry,
                tradingNames,
                bankAccountCountry,
                identificationMemberState,
                firstDeliveryDate,
                desiredStartDate,
                electronicInterfaceFlag,
                notEstablishedInEuDeclaration,
                existingDanishVatNumber
        );
    }

    /** POST to /api/v1/registrations and capture response + status. */
    private void submitRegistration() {
        RegistrationRequest request = buildRequest();
        lastRegistrationResponse = restTemplate.postForEntity(
                registrationsUrl(), request, RegistrationResponse.class);
        lastHttpStatus = lastRegistrationResponse.getStatusCode().value();
        if (lastRegistrationResponse.getStatusCode().is2xxSuccessful()
                && lastRegistrationResponse.getBody() != null) {
            currentRegistrationId = lastRegistrationResponse.getBody().registrationId();
        } else {
            lastErrorBody = lastRegistrationResponse.toString();
        }
    }

    /** GET /api/v1/registrations/{id} and refresh lastRegistrationResponse. */
    private void fetchRegistration(UUID id) {
        lastRegistrationResponse = restTemplate.getForEntity(
                registrationUrl(id), RegistrationResponse.class);
        lastHttpStatus = lastRegistrationResponse.getStatusCode().value();
    }

    /** POST action (approve / reject / exclude / deregister) with a body. */
    private ResponseEntity<RegistrationResponse> postAction(UUID id, String action, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        ResponseEntity<RegistrationResponse> resp = restTemplate.exchange(
                actionUrl(id, action), HttpMethod.POST, entity, RegistrationResponse.class);
        lastHttpStatus = resp.getStatusCode().value();
        lastActionResponse = resp;
        return resp;
    }

    /** POST action with a plain Map body (for endpoints whose request DTO has no Java record yet). */
    private ResponseEntity<String> postActionRaw(UUID id, String action, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> resp = restTemplate.exchange(
                actionUrl(id, action), HttpMethod.POST, entity, String.class);
        lastHttpStatus = resp.getStatusCode().value();
        return resp;
    }

    /**
     * Convenience: create a pre-existing ACTIVE registration for scenarios that
     * require "Given a taxable person has/with an active [scheme] registration".
     *
     * Submit → approve (with a placeholder VAT number) → fetch.
     * COMPILE-TIME FAILURE: ApprovalRequest record does not exist yet.
     */
    private void createActiveRegistration(String schemeType) {
        scheme = schemeType;
        if ("EU".equals(schemeType)) {
            homeCountry = "DK";
            homeCountryTaxNumber = null;
        }
        submitRegistration();
        assertThat(currentRegistrationId)
                .as("Pre-existing registration must be created before approving — "
                    + "endpoint POST /api/v1/registrations must be implemented first")
                .isNotNull();
        // Approve immediately to move to ACTIVE
        ApprovalRequest approval = new ApprovalRequest("DKTEST" + currentRegistrationId.toString().substring(0, 8).toUpperCase());
        postAction(currentRegistrationId, "approve", approval);
        // Re-fetch so lastRegistrationResponse reflects ACTIVE state
        fetchRegistration(currentRegistrationId);
    }

    /** Assert that the most recent registration response body is non-null. */
    private RegistrationResponse body() {
        assertThat(lastRegistrationResponse)
                .as("A When step must execute before Then assertions")
                .isNotNull();
        assertThat(lastRegistrationResponse.getBody())
                .as("Response body must not be null — endpoint not yet implemented")
                .isNotNull();
        return lastRegistrationResponse.getBody();
    }

    /** Assert that the most recent action response body is non-null. */
    private RegistrationResponse actionBody() {
        assertThat(lastActionResponse)
                .as("An action When step must execute before Then assertions")
                .isNotNull();
        assertThat(lastActionResponse.getBody())
                .as("Action response body must not be null")
                .isNotNull();
        return lastActionResponse.getBody();
    }

    // =========================================================================
    // ── BACKGROUND steps ─────────────────────────────────────────────────────
    // =========================================================================

    /**
     * FR: All scenarios — Denmark is the identification member state.
     */
    @Given("Denmark is the identification member state for all registrations in this feature")
    public void denmarkIsIdentificationMemberState() {
        identificationMemberState = "DK";
    }

    /**
     * Acknowledges that this test suite targets the authoritative registration DB.
     * Triggers a connectivity check so a missing application context fails fast.
     *
     * INTENTIONAL COMPILE-TIME FAILURE: RegistrationRequest/RegistrationResponse
     * imports above prevent compilation until production code is created.
     */
    @And("the system is the authoritative registration database for Skatteforvaltningen")
    public void systemIsAuthoritativeRegistrationDatabase() {
        // Connectivity assertion — validates the application started on the random port.
        assertThat(port)
                .as("Spring context must start on a random port (Testcontainers PostgreSQL required)")
                .isGreaterThan(0);
    }

    // =========================================================================
    // ── Feature: Non-EU scheme registration lifecycle ─────────────────────────
    // =========================================================================

    // ── Scenario 1: Non-EU registration accepted for eligible applicant ────────

    /**
     * FR-OSS-02-001 / FR-OSS-02-002
     */
    @Given("a taxable person named {string} based in a non-EU country")
    public void aTaxablePersonNamedBasedInNonEuCountry(String name) {
        registrantName = name;
        scheme = "NON_EU";
        homeCountry = "US";
    }

    @And("they declare no fixed establishment in any EU member state")
    public void theyDeclareNoFixedEstablishment() {
        hasEuFixedEstablishment = false;
    }

    @When("they submit a complete electronic Non-EU scheme registration notification on {localdate}")
    public void submitCompleteNonEuRegistrationOn(LocalDate date) {
        desiredStartDate = date;
        submitRegistration();
    }

    @Then("the registration is accepted with status {string}")
    public void registrationAcceptedWithStatus(String expectedStatus) {
        assertThat(lastHttpStatus)
                .as("POST /api/v1/registrations must return 2xx — endpoint not yet implemented")
                .isBetween(200, 299);
        assertThat(body().status())
                .as("Registration status must be [%s] — status machine not yet implemented", expectedStatus)
                .isEqualTo(expectedStatus);
    }

    @And("the desired start date {string} is stored against the registration")
    public void desiredStartDateStoredAgainstRegistration(String dateStr) {
        LocalDate expected = LocalDate.parse(dateStr);
        assertThat(body().effectiveDate())
                .as("Effective/desired-start date [%s] must be stored — date rule not yet implemented", expected)
                .isEqualTo(expected);
    }

    // ── Scenario 2: Non-EU registration rejected when EU fixed establishment ───

    /** FR-OSS-02-002 */
    @Given("a taxable person who has a fixed establishment in Germany")
    public void taxablePersonWithFixedEstablishmentInGermany() {
        hasEuFixedEstablishment = true;
        euFixedEstablishmentCountry = "DE";
    }

    @When("they attempt to register under the Non-EU scheme")
    public void attemptRegisterNonEuScheme() {
        scheme = "NON_EU";
        submitRegistration();
    }

    @Then("the registration is rejected with reason {string}")
    public void registrationRejectedWithReason(String reason) {
        assertThat(lastHttpStatus)
                .as("Registration with EU establishment must be rejected (4xx) — validation not yet implemented")
                .isBetween(400, 499);
        assertThat(lastErrorBody)
                .as("Error body must contain rejection reason [%s]", reason)
                .contains(reason);
    }

    @And("no registration record is created")
    public void noRegistrationRecordCreated() {
        assertThat(currentRegistrationId)
                .as("No registrationId should be stored when submission is rejected")
                .isNull();
    }

    // ── Scenarios 3-4: Normal effective date calculation ──────────────────────

    /** FR-OSS-02-003 */
    @Given("a taxable person submits a Non-EU scheme registration notification on {localdate}")
    public void taxablePersonSubmitsNonEuNotificationOn(LocalDate date) {
        desiredStartDate = date;
        notificationDate = date;
        scheme = "NON_EU";
    }

    @And("no eligible delivery has been made before the notification date")
    public void noEligibleDeliveryBeforeNotificationDate() {
        firstDeliveryDate = null;
    }

    @When("the system calculates the effective date")
    public void systemCalculatesEffectiveDate() {
        submitRegistration();
    }

    @Then("the registration effective date is {localdate}")
    public void registrationEffectiveDateIs(LocalDate expected) {
        assertThat(body().effectiveDate())
                .as("Registration effective date must be [%s] — quarter-rule not yet implemented", expected)
                .isEqualTo(expected);
    }

    // ── Scenarios 5-6: Early-delivery exception applies ───────────────────────

    /** FR-OSS-02-004 */
    @Given("a taxable person makes their first eligible Non-EU scheme delivery on {localdate}")
    public void firstEligibleNonEuDeliveryOn(LocalDate date) {
        firstDeliveryDate = date;
        scheme = "NON_EU";
    }

    @When("they submit the registration notification on {localdate}")
    public void submitRegistrationNotificationOn(LocalDate date) {
        notificationDate = date;
        desiredStartDate = date;
        submitRegistration();
    }

    @Then("the early-delivery exception is applied")
    public void earlyDeliveryExceptionApplied() {
        assertThat(body().earlyDeliveryException())
                .as("Early-delivery exception flag must be true — ML § 66b stk. 3 not yet implemented")
                .isTrue();
    }

    // ── Scenario 7: Late notification forfeits early-delivery exception ────────

    /** FR-OSS-02-005 */
    @Then("the early-delivery exception is NOT applied")
    public void earlyDeliveryExceptionNotApplied() {
        assertThat(body().earlyDeliveryException())
                .as("Early-delivery exception must NOT be applied — ML § 66b stk. 3 deadline check not yet implemented")
                .isFalse();
    }

    // ── Scenario 8: Missing mandatory field rejected ──────────────────────────

    /** FR-OSS-02-009 */
    @Given("a taxable person attempts to register under the Non-EU scheme")
    public void taxablePersonAttemptsNonEuRegistration() {
        scheme = "NON_EU";
    }

    @When("the registration submission is missing the {string} field")
    public void registrationSubmissionMissingField(String field) {
        missingField = field;
        submitRegistration();
    }

    @Then("the system rejects the submission with validation error {string}")
    public void systemRejectsSubmissionWithValidationError(String errorCode) {
        assertThat(lastHttpStatus)
                .as("Missing-field submission must be rejected (400) — @Valid not yet wired")
                .isEqualTo(400);
        assertThat(lastErrorBody)
                .as("Validation error response must contain error code [%s]", errorCode)
                .contains(errorCode.split(":")[0]);
    }

    // ── Scenario 9: All 9 mandatory fields present ────────────────────────────

    /** FR-OSS-02-009 */
    @Given("a taxable person submits a Non-EU scheme registration with all mandatory fields:")
    public void taxablePersonSubmitsWithAllMandatoryFields(DataTable table) {
        Map<String, String> fields = table.asMap(String.class, String.class);
        registrantName       = fields.getOrDefault("name", registrantName);
        tradingNames         = List.of(fields.getOrDefault("trading_names", "Acme Digital"));
        homeCountry          = fields.getOrDefault("home_country", homeCountry);
        homeCountryTaxNumber = fields.getOrDefault("home_country_tax_number", homeCountryTaxNumber);
        postalAddress        = fields.getOrDefault("postal_address", postalAddress);
        email                = fields.getOrDefault("email", email);
        phoneNumber          = fields.getOrDefault("phone", phoneNumber);
        bankDetails          = fields.getOrDefault("bank_details", bankDetails);
        scheme               = "NON_EU";
    }

    @When("the submission is processed")
    public void submissionIsProcessed() {
        submitRegistration();
    }

    // ── Scenarios 10-11: VAT number assigned within 8 days ───────────────────

    /** FR-OSS-02-013 */
    @Given("a complete Non-EU scheme registration is received on {localdate}")
    public void completeNonEuRegistrationReceivedOn(LocalDate date) {
        receiptDate = date;
        desiredStartDate = date;
        scheme = "NON_EU";
        // Submit the registration so we have a currentRegistrationId
        submitRegistration();
    }

    /** FR-OSS-02-013 — "7 calendar days elapsed" modelled as triggering approval */
    @When("{int} calendar days have elapsed since receipt")
    public void calendarDaysElapsedSinceReceipt(int days) {
        assertThat(currentRegistrationId)
                .as("Registration must be submitted before simulating day-elapsed processing")
                .isNotNull();
        // Simulate caseworker approval with VAT number assignment
        // COMPILE-TIME FAILURE: ApprovalRequest does not exist yet
        String placeholderVat = "DKTEST" + currentRegistrationId.toString().substring(0, 8).toUpperCase();
        ApprovalRequest approval = new ApprovalRequest(placeholderVat);
        postAction(currentRegistrationId, "approve", approval);
        fetchRegistration(currentRegistrationId);
    }

    @Then("the system has assigned a unique VAT registration number")
    public void systemHasAssignedUniqueVatNumber() {
        assertThat(body().vatNumber())
                .as("VAT number must be assigned and non-null — approval endpoint not yet implemented")
                .isNotNull()
                .isNotBlank();
    }

    @And("the number is communicated electronically to the taxable person")
    public void numberCommunicatedElectronically() {
        // OQ-4: assert HTTP 200 on the approval action, not delivery channel
        assertThat(lastHttpStatus)
                .as("Approval/notification must return 2xx — not yet implemented")
                .isBetween(200, 299);
    }

    @And("the number is flagged as {string}")
    public void numberFlaggedAs(String flag) {
        assertThat(body().vatNumberFlag())
                .as("VAT number flag must be [%s] — flag logic not yet implemented", flag)
                .isEqualTo(flag);
    }

    @And("the registration status is {string}")
    public void registrationStatusIs(String expected) {
        assertThat(body().status())
                .as("Registration status must be [%s] — status not yet implemented", expected)
                .isEqualTo(expected);
    }

    // ── Scenario 11: Delay notification ──────────────────────────────────────

    /** FR-OSS-02-014 */
    @And("the system cannot complete the number assignment by {localdate}")
    public void systemCannotCompleteNumberAssignmentBy(LocalDate deadline) {
        // Sets scenario context; actual block is checked in the When step
        this.notificationDate = deadline;
    }

    @When("{localdate} is reached \\(day 8 boundary\\)")
    public void day8BoundaryReached(LocalDate boundary) {
        assertThat(currentRegistrationId)
                .as("Registration must exist before triggering day-8 boundary check")
                .isNotNull();
        // Call a system evaluation endpoint to trigger delay-notification logic
        Map<String, Object> evalBody = new HashMap<>();
        evalBody.put("evaluationDate", boundary.toString());
        evalBody.put("checkDeadlineNotification", true);
        postActionRaw(currentRegistrationId, "evaluate", evalBody);
        fetchRegistration(currentRegistrationId);
    }

    @Then("the system sends an electronic delay notification to the taxable person")
    public void systemSendsElectronicDelayNotification() {
        assertThat(body().delayNotificationSent())
                .as("Delay notification flag must be true — day-8 check not yet implemented")
                .isTrue();
    }

    @And("the notification states the expected assignment date")
    public void notificationStatesExpectedAssignmentDate() {
        assertThat(body().expectedAssignmentDate())
                .as("Expected assignment date must be present in response — not yet implemented")
                .isNotNull();
    }

    // ── Scenario 12: Non-EU VAT number rejected for ordinary use ─────────────

    /** FR-OSS-02-015 */
    @Given("a taxable person holds a Non-EU scheme VAT number {string}")
    public void taxablePersonHoldsNonEuVatNumber(String vatNumber) {
        vatNumberToUse = vatNumber;
    }

    @When("they attempt to use {string} for an ordinary Danish VAT return")
    public void attemptUseVatNumberForOrdinaryReturn(String vatNumber) {
        Map<String, Object> body = new HashMap<>();
        body.put("vatNumber", vatNumber);
        body.put("useContext", "ORDINARY_VAT_RETURN");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> resp = restTemplate.exchange(
                "http://localhost:" + port + "/api/v1/registrations/validate-vat-number-use",
                HttpMethod.POST, entity, String.class);
        lastHttpStatus = resp.getStatusCode().value();
        lastErrorBody = resp.getBody();
    }

    @Then("the system rejects the use with reason {string}")
    public void systemRejectsUseWithReason(String reason) {
        assertThat(lastHttpStatus)
                .as("Ordinary-VAT use of Non-EU number must be rejected (4xx) — restriction not yet implemented")
                .isBetween(400, 499);
        // reason = "NUMBER_RESTRICTED_TO_NON_EU_SCHEME"
        String reasonPrefix = reason.split("_")[0]; // "NUMBER"
        assertThat(lastErrorBody != null ? lastErrorBody : "")
                .as("Rejection body must contain keyword from reason [%s]", reason)
                .containsIgnoringCase(reasonPrefix);
    }

    // =========================================================================
    // ── Feature: EU scheme registration lifecycle ─────────────────────────────
    // =========================================================================

    // ── Scenario 13: EU registration accepted ────────────────────────────────

    /** FR-OSS-02-006 */
    @Given("a taxable person established in Denmark makes intra-EU distance sales of goods")
    public void taxablePersonInDenmarkMakesIntraEuDistanceSales() {
        scheme = "EU";
        homeCountry = "DK";
        homeCountryTaxNumber = null; // not required for DK-established
    }

    @When("they submit a complete EU scheme registration notification on {localdate}")
    public void submitCompleteEuRegistrationNotificationOn(LocalDate date) {
        desiredStartDate = date;
        notificationDate = date;
        submitRegistration();
    }

    @Then("the effective date is {localdate}")
    public void effectiveDateIs(LocalDate expected) {
        assertThat(body().effectiveDate())
                .as("Effective date must be [%s] — quarter-rule not yet implemented", expected)
                .isEqualTo(expected);
    }

    // ── Scenarios 14-15: EU early-delivery exception ──────────────────────────

    /** FR-OSS-02-007 */
    @Given("a taxable person makes their first eligible EU scheme delivery on {localdate}")
    public void firstEligibleEuDeliveryOn(LocalDate date) {
        firstDeliveryDate = date;
        scheme = "EU";
        homeCountry = "DK";
        homeCountryTaxNumber = null;
    }

    // ── Scenario 16: EU registration rejected when §117 field missing ─────────

    /** FR-OSS-02-010 */
    @Given("a taxable person attempts to register under the EU scheme")
    public void taxablePersonAttemptsEuRegistration() {
        scheme = "EU";
        homeCountry = "DK";
        homeCountryTaxNumber = null;
    }

    // ── Scenario 17: Electronic-interface declaration required ────────────────

    /** FR-OSS-02-011 */
    @Given("a taxable person is an electronic interface under ML § 4c stk. 2")
    public void taxablePersonIsElectronicInterface() {
        electronicInterfaceFlag = false; // deliberately NOT set → will be missing in next When
        scheme = "EU";
    }

    @When("they submit an EU scheme registration without the electronic-interface flag set")
    public void submitEuRegistrationWithoutElectronicInterfaceFlag() {
        // electronicInterfaceFlag remains false (not set), missingField tracks the logical gap
        missingField = "electronic_interface";
        scheme = "EU";
        submitRegistration();
    }

    // ── Scenario 18: Non-EU-established with declaration ─────────────────────

    /** FR-OSS-02-012 */
    @Given("a taxable person is not established in the EU")
    public void taxablePersonNotEstablishedInEu() {
        notEstablishedInEuDeclaration = false; // will be set to true in next step
        scheme = "EU";
    }

    @And("they submit an EU scheme registration with the {string} declaration set to true")
    public void euRegistrationWithDeclarationSet(String declarationName) {
        if ("not_established_in_eu".equals(declarationName)) {
            notEstablishedInEuDeclaration = true;
        }
    }

    @And("all other mandatory fields from §117 stk. 1 are present")
    public void allOtherMandatoryFieldsPresent() {
        // All default world-state fields are already valid; nothing to override.
        assertThat(registrantName).isNotBlank();
        assertThat(email).isNotBlank();
        assertThat(postalAddress).isNotBlank();
    }

    // ── Scenario 19: Existing Danish VAT number ───────────────────────────────

    /** FR-OSS-02-016 */
    @Given("a taxable person is already VAT-registered in Denmark with number {string}")
    public void taxablePersonAlreadyVatRegisteredInDenmark(String vatNumber) {
        hasDanishVatRegistration = true;
        existingDanishVatNumber = vatNumber;
        homeCountry = "DK";
        homeCountryTaxNumber = vatNumber;
        scheme = "EU";
    }

    @When("they successfully register for the EU scheme")
    public void successfullyRegisterForEuScheme() {
        submitRegistration();
        assertThat(lastHttpStatus)
                .as("EU scheme registration must succeed — endpoint not yet implemented")
                .isBetween(200, 299);
    }

    @Then("no new VAT number is assigned")
    public void noNewVatNumberAssigned() {
        assertThat(body().vatNumber())
                .as("Existing Danish VAT number must be reused — assignment logic not yet implemented")
                .isEqualTo(existingDanishVatNumber);
    }

    @And("the EU scheme registration is linked to {string}")
    public void euSchemeRegistrationLinkedTo(String vatNumber) {
        assertThat(body().vatNumber())
                .as("EU scheme must be linked to existing VAT number [%s]", vatNumber)
                .isEqualTo(vatNumber);
    }

    // ── Scenario 20: EU applicant without Danish VAT gets new number ──────────

    /** FR-OSS-02-017 */
    @Given("a taxable person is not VAT-registered in Denmark under ordinary rules")
    public void taxablePersonNotVatRegisteredInDenmark() {
        hasDanishVatRegistration = false;
        existingDanishVatNumber = null;
        scheme = "EU";
        homeCountry = "DK";
        homeCountryTaxNumber = null;
    }

    @And("their complete EU scheme registration information is received on {localdate}")
    public void completeEuRegistrationInformationReceivedOn(LocalDate date) {
        receiptDate = date;
        desiredStartDate = date;
        submitRegistration();
    }

    // ── Scenario 21: EU number retained when ordinary Danish VAT ceases ───────

    /** FR-OSS-02-018 */
    @Given("a taxable person is registered for the EU scheme using their Danish VAT number {string}")
    public void registeredForEuSchemeUsingDanishVatNumber(String vatNumber) {
        existingDanishVatNumber = vatNumber;
        hasDanishVatRegistration = true;
        scheme = "EU";
        homeCountry = "DK";
        createActiveRegistration("EU");
    }

    @When("they cease their ordinary Danish VAT registration")
    public void ceaseOrdinaryDanishVatRegistration() {
        Map<String, Object> body = new HashMap<>();
        body.put("event", "ORDINARY_VAT_DEREGISTRATION");
        body.put("affectedVatNumber", existingDanishVatNumber);
        postActionRaw(currentRegistrationId, "notify-vat-change", body);
        fetchRegistration(currentRegistrationId);
    }

    @And("they are not simultaneously deregistering from the EU scheme")
    public void notSimultaneouslyDeregisteringFromEuScheme() {
        simultaneousDeregistration = false;
        // Verification: fetch current status; must still be ACTIVE
        assertThat(body().status())
                .as("EU scheme must remain ACTIVE during ordinary-VAT deregistration — not yet implemented")
                .isEqualTo("ACTIVE");
    }

    @Then("the system assigns a new individual EU scheme VAT number to replace {string}")
    public void systemAssignsNewEuVatNumberToReplace(String oldVatNumber) {
        fetchRegistration(currentRegistrationId);
        assertThat(body().vatNumber())
                .as("A new EU scheme VAT number must be assigned to replace [%s] — not yet implemented", oldVatNumber)
                .isNotNull()
                .isNotEqualTo(oldVatNumber);
    }

    @And("the EU scheme registration remains ACTIVE")
    public void euSchemeRegistrationRemainsActive() {
        assertThat(body().status())
                .as("EU scheme registration must remain ACTIVE — status machine not yet implemented")
                .isEqualTo("ACTIVE");
    }

    // =========================================================================
    // ── Feature: EU scheme identification member state binding rule ───────────
    // =========================================================================

    // ── Scenario 22: Binding period set correctly ─────────────────────────────

    /** FR-OSS-02-020 */
    @Given("a taxable person registers for the EU scheme in calendar year {int}")
    public void registersForEuSchemeInCalendarYear(int year) {
        registrationCalendarYear = year;
        desiredStartDate = LocalDate.of(year, 5, 10);
        scheme = "EU";
        homeCountry = "SE"; // not DK → case b or c applies
    }

    @And("they selected Denmark as identification member state under ML § 66d stk. 1 case b")
    public void selectedDenmarkUnderCase66dStk1CaseB() {
        bindingMemberStateCase = "b";
        identificationMemberState = "DK";
    }

    @When("the registration is confirmed")
    public void registrationIsConfirmed() {
        submitRegistration();
        assertThat(lastHttpStatus)
                .as("Registration confirmation must return 2xx — not yet implemented")
                .isBetween(200, 299);
    }

    @Then("the binding period end date is {localdate}")
    public void bindingPeriodEndDateIs(LocalDate expected) {
        assertThat(body().bindingPeriodEnd())
                .as("Binding period end date must be [%s] — ML § 66d stk. 2 not yet implemented", expected)
                .isEqualTo(expected);
    }

    @And("the system stores the binding rule type as {string}")
    public void systemStoresBindingRuleTypeAs(String expected) {
        assertThat(body().bindingRuleType())
                .as("Binding rule type must be [%s] — not yet implemented", expected)
                .isEqualTo(expected);
    }

    // ── Scenario 23: IMS change blocked during binding period ─────────────────

    /** FR-OSS-02-021 */
    @Given("a taxable person is bound to Denmark as EU scheme identification member state until {localdate}")
    public void boundToDenmarkUntil(LocalDate endDate) {
        bindingPeriodEndDate = endDate;
        scheme = "EU";
        homeCountry = "SE";
        createActiveRegistration("EU");
    }

    @When("they attempt to change identification member state to France on {localdate}")
    public void attemptChangeIdentificationMemberStateTo(LocalDate changeDate) {
        Map<String, Object> body = new HashMap<>();
        body.put("newIdentificationMemberState", "FR");
        body.put("requestedChangeDate", changeDate.toString());
        postActionRaw(currentRegistrationId, "change-identification-member-state", body);
    }

    @Then("the system rejects the change with reason {string}")
    public void systemRejectsChangeWithReason(String reason) {
        assertThat(lastHttpStatus)
                .as("Binding-period change must be rejected (4xx) — not yet implemented")
                .isBetween(400, 499);
        assertThat(lastErrorBody != null ? lastErrorBody : "")
                .as("Error body must contain rejection reason keyword from [%s]", reason)
                .containsIgnoringCase("BOUND");
    }

    // ── Scenario 24: Case a — no binding period ───────────────────────────────

    /** FR-OSS-02-022 */
    @Given("a taxable person's home establishment is in Denmark (case a)")
    public void homeEstablishmentInDenmarkCaseA() {
        bindingMemberStateCase = "a";
        homeCountry = "DK";
        scheme = "EU";
        identificationMemberState = "DK";
    }

    @When("they register for the EU scheme")
    public void registerForEuScheme() {
        submitRegistration();
        assertThat(lastHttpStatus)
                .as("EU registration must succeed — endpoint not yet implemented")
                .isBetween(200, 299);
    }

    @Then("no binding period is recorded")
    public void noBindingPeriodRecorded() {
        assertThat(body().bindingPeriodEnd())
                .as("Binding period end must be null for case a (DK home establishment) — not yet implemented")
                .isNull();
    }

    @And("the binding rule type is {string}")
    public void bindingRuleTypeIs(String expected) {
        assertThat(body().bindingRuleType())
                .as("Binding rule type must be [%s]", expected)
                .isEqualTo(expected);
    }

    // ── Scenario 25: Binding rule not blocking when DK ceases to qualify ──────

    /** FR-OSS-02-021 (permitted change) */
    @Given("a taxable person is bound to Denmark as identification member state under the EU scheme until {localdate}")
    public void boundToDenmarkUnderEuUntil(LocalDate endDate) {
        bindingPeriodEndDate = endDate;
        scheme = "EU";
        homeCountry = "SE";
        createActiveRegistration("EU");
    }

    @And("their home establishment moves to France on {localdate}")
    public void homeEstablishmentMovesToFranceOn(LocalDate date) {
        homeEstablishmentMoveDate = date;
        newIdentificationMemberState = "FR";
    }

    @When("they notify Skatteforvaltningen of the change on {localdate}")
    public void notifySkatteforvaltningenOfChangeOn(LocalDate date) {
        notificationDate = date;
        Map<String, Object> body = new HashMap<>();
        body.put("newIdentificationMemberState", "FR");
        body.put("establishmentMoveDate", homeEstablishmentMoveDate.toString());
        body.put("notificationDate", date.toString());
        postActionRaw(currentRegistrationId, "change-identification-member-state", body);
        fetchRegistration(currentRegistrationId);
    }

    @Then("the system permits the identification member state change")
    public void systemPermitsIdentificationMemberStateChange() {
        assertThat(lastHttpStatus)
                .as("Change permitted when Denmark no longer qualifies — must return 2xx (not yet implemented)")
                .isBetween(200, 299);
    }

    @And("the change is effective {localdate}")
    public void changeIsEffective(LocalDate expected) {
        assertThat(body().effectiveDate())
                .as("Change effective date must be [%s] — not yet implemented", expected)
                .isEqualTo(expected);
    }

    @And("the binding period clock resets in France from {localdate}")
    public void bindingPeriodClockResetsInFranceFrom(LocalDate resetDate) {
        // The Danish registration closes; the response should reflect the closure date.
        assertThat(body().effectiveDate())
                .as("Binding clock reset date (registration closure) must be [%s]", resetDate)
                .isEqualTo(resetDate);
    }

    // =========================================================================
    // ── Feature: Registration change notifications ────────────────────────────
    // =========================================================================

    /** FR-OSS-02-023 */
    @Given("a taxable person with an active Non-EU scheme registration")
    public void taxablePersonWithActiveNonEuRegistration() {
        createActiveRegistration("NON_EU");
    }

    @And("a change to their postal address occurred on {localdate}")
    public void changeToPostalAddressOccurredOn(LocalDate date) {
        fieldToUpdate = "postal_address";
        newFieldValue = "999 New Street, Los Angeles, USA";
        fieldChangeDate = date;
    }

    @When("they submit the change notification on {localdate}")
    public void submitChangeNotificationOn(LocalDate date) {
        notificationDate = date;
        Map<String, Object> body = new HashMap<>();
        body.put("field", fieldToUpdate);
        body.put("newValue", newFieldValue);
        body.put("changeDate", fieldChangeDate.toString());
        body.put("notificationDate", date.toString());
        postActionRaw(currentRegistrationId, "notify-change", body);
        fetchRegistration(currentRegistrationId);
    }

    @Then("the system updates the postal address")
    public void systemUpdatesPostalAddress() {
        assertThat(lastHttpStatus)
                .as("Change notification must succeed (2xx) — PATCH endpoint not yet implemented")
                .isBetween(200, 299);
    }

    @And("the notification is recorded as timely")
    public void notificationRecordedAsTimely() {
        assertThat(body().changeNotificationTimely())
                .as("Change notification must be flagged as timely — compliance check not yet implemented")
                .isTrue();
    }

    /** FR-OSS-02-024 */
    @And("a change to their phone number occurred on {localdate}")
    public void changeToPhoneNumberOccurredOn(LocalDate date) {
        fieldToUpdate = "phone_number";
        newFieldValue = "+1-555-9999";
        fieldChangeDate = date;
    }

    @Then("the system updates the phone number")
    public void systemUpdatesPhoneNumber() {
        assertThat(lastHttpStatus)
                .as("Phone number update must succeed (2xx) — not yet implemented")
                .isBetween(200, 299);
    }

    @And("the notification is flagged as {string}")
    public void notificationFlaggedAs(String flag) {
        assertThat(body().complianceFlags())
                .as("Compliance flag [%s] must be present — late-notification check not yet implemented", flag)
                .contains(flag);
    }

    @And("the flag is visible in the compliance review queue")
    public void flagVisibleInComplianceReviewQueue() {
        assertThat(body().complianceFlags())
                .as("Compliance flag must be non-empty — compliance queue not yet implemented")
                .isNotEmpty();
    }

    /** FR-OSS-02-025 */
    @Given("a taxable person with an active EU scheme registration")
    public void taxablePersonWithActiveEuRegistration() {
        createActiveRegistration("EU");
    }

    @And("their business eligible for the scheme ceased on {localdate}")
    public void businessEligibleForSchemeCeasedOn(LocalDate date) {
        cessationDate = date;
    }

    @When("they submit the cessation notification on {localdate}")
    public void submitCessationNotificationOn(LocalDate date) {
        notificationDate = date;
        Map<String, Object> body = new HashMap<>();
        body.put("cessationDate", cessationDate.toString());
        body.put("notificationDate", date.toString());
        postActionRaw(currentRegistrationId, "notify-cessation", body);
        fetchRegistration(currentRegistrationId);
    }

    @Then("the system records the cessation notification as timely")
    public void systemRecordsCessationNotificationAsTimely() {
        assertThat(lastHttpStatus)
                .as("Cessation notification must succeed — endpoint not yet implemented")
                .isBetween(200, 299);
        assertThat(body().changeNotificationTimely())
                .as("Cessation notification must be recorded as timely — 10-day check not yet implemented")
                .isTrue();
    }

    @And("the registration is moved to status {string}")
    public void registrationMovedToStatus(String expected) {
        assertThat(body().status())
                .as("Registration status after cessation must be [%s] — status machine not yet implemented", expected)
                .isEqualTo(expected);
    }

    /** FR-OSS-02-026 */
    @Given("a taxable person registered under the EU scheme with Denmark as identification member state")
    public void taxablePersonRegisteredUnderEuSchemeWithDenmark() {
        scheme = "EU";
        homeCountry = "DK";
        identificationMemberState = "DK";
        createActiveRegistration("EU");
    }

    @And("their home establishment moved to Sweden on {localdate}")
    public void homeEstablishmentMovedToSwedenOn(LocalDate date) {
        homeEstablishmentMoveDate = date;
        newIdentificationMemberState = "SE";
    }

    @When("they notify Skatteforvaltningen and Sweden of the change on {localdate}")
    public void notifySkatteforvaltningenAndSwedenOn(LocalDate date) {
        notificationDate = date;
        Map<String, Object> body = new HashMap<>();
        body.put("newIdentificationMemberState", "SE");
        body.put("establishmentMoveDate", homeEstablishmentMoveDate.toString());
        body.put("notificationDate", date.toString());
        body.put("notifyNewMemberState", true);
        postActionRaw(currentRegistrationId, "change-identification-member-state", body);
        fetchRegistration(currentRegistrationId);
    }

    @Then("Denmark records the identification member state change effective {localdate}")
    public void denmarkRecordsIdentificationMemberStateChangeEffective(LocalDate expected) {
        assertThat(body().effectiveDate())
                .as("IMS change effective date must be [%s] — not yet implemented", expected)
                .isEqualTo(expected);
    }

    @And("the system confirms the outgoing notification to Sweden was dispatched")
    public void systemConfirmsOutgoingNotificationToSwedenDispatched() {
        assertThat(body().outgoingMemberStateNotificationDispatched())
                .as("Outgoing notification to new IMS must be flagged as dispatched — not yet implemented")
                .isTrue();
    }

    @And("the taxable person's registration in Denmark is closed as of {localdate}")
    public void denmarkRegistrationClosedAsOf(LocalDate expected) {
        assertThat(body().closedDate())
                .as("Registration closure date in Denmark must be [%s] — not yet implemented", expected)
                .isEqualTo(expected);
    }

    // =========================================================================
    // ── Feature: Voluntary deregistration ────────────────────────────────────
    // =========================================================================

    /** FR-OSS-02-027 */
    @Given("a taxable person has an active Non-EU scheme registration")
    public void taxablePersonHasActiveNonEuRegistration() {
        createActiveRegistration("NON_EU");
    }

    @When("they submit a voluntary deregistration notification on {localdate}")
    public void submitVoluntaryDeregistrationNotificationOn(LocalDate date) {
        deregistrationNotificationDate = date;
        // COMPILE-TIME FAILURE: DeregistrationRequest record does not exist yet
        DeregistrationRequest request = new DeregistrationRequest(date, "VOLUNTARY");
        lastActionResponse = restTemplate.postForEntity(
                actionUrl(currentRegistrationId, "deregister"),
                request, RegistrationResponse.class);
        lastHttpStatus = lastActionResponse.getStatusCode().value();
        fetchRegistration(currentRegistrationId);
    }

    @Then("the deregistration is recorded as timely \\(15 or more days before {localdate}\\)")
    public void deregistrationRecordedAsTimely15OrMoreDaysBefore(LocalDate quarterEnd) {
        assertThat(body().deregistrationTimely())
                .as("Deregistration must be timely (≥15 days before %s) — 15-day check not yet implemented", quarterEnd)
                .isTrue();
    }

    @And("the deregistration effective date is {localdate}")
    public void deregistrationEffectiveDateIs(LocalDate expected) {
        assertThat(body().deregistrationEffectiveDate())
                .as("Deregistration effective date must be [%s] — quarter-rule not yet implemented", expected)
                .isEqualTo(expected);
    }

    /** FR-OSS-02-028 */
    @Given("a taxable person has an active EU scheme registration")
    public void taxablePersonHasActiveEuRegistration() {
        createActiveRegistration("EU");
    }

    @Then("the deregistration is recorded as timely \\(exactly 15 days before {localdate}\\)")
    public void deregistrationRecordedAsTimelyExactly15DaysBefore(LocalDate quarterEnd) {
        assertThat(body().deregistrationTimely())
                .as("Deregistration must be timely (exactly 15 days before %s) — inclusive boundary not yet implemented",
                        quarterEnd)
                .isTrue();
    }

    /** FR-OSS-02-029 */
    @Then("{localdate} is fewer than 15 days before {localdate}")
    public void dateIsFewerThan15DaysBefore(LocalDate notificationDate, LocalDate quarterEnd) {
        // Assertive documentation step — verifies the test precondition is correctly modelled
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(notificationDate, quarterEnd);
        assertThat(daysBetween)
                .as("[%s] must be fewer than 15 days before [%s] (gap=%d days)", notificationDate, quarterEnd, daysBetween)
                .isLessThan(15);
    }

    @And("the deregistration effective date is deferred to {localdate}")
    public void deregistrationEffectiveDateDeferredTo(LocalDate expected) {
        assertThat(body().deregistrationEffectiveDate())
                .as("Deferred deregistration effective date must be [%s] — deferral rule not yet implemented", expected)
                .isEqualTo(expected);
    }

    @And("the taxable person is notified of the revised effective date")
    public void taxablePersonNotifiedOfRevisedEffectiveDate() {
        // OQ-4: assert HTTP 2xx on the deregistration call, not notification channel
        assertThat(lastHttpStatus)
                .as("Revised deregistration date notification requires 2xx — not yet implemented")
                .isBetween(200, 299);
    }

    /** FR-OSS-02-030 — supply within deregistration window */
    @Given("a taxable person submits a timely voluntary deregistration effective {localdate}")
    public void taxablePersonSubmitsTimelyVoluntaryDeregistrationEffective(LocalDate effectiveDate) {
        createActiveRegistration("NON_EU");
        DeregistrationRequest request = new DeregistrationRequest(effectiveDate, "VOLUNTARY");
        postAction(currentRegistrationId, "deregister", request);
        fetchRegistration(currentRegistrationId);
    }

    @When("they make an eligible supply on {localdate}")
    public void makeEligibleSupplyOn(LocalDate date) {
        supplyDate = date;
        Map<String, Object> body = new HashMap<>();
        body.put("supplyDate", date.toString());
        body.put("registrationId", currentRegistrationId.toString());
        postActionRaw(currentRegistrationId, "record-supply", body);
    }

    @Then("the system accepts the supply as covered by the scheme")
    public void systemAcceptsSupplyAsCoveredByScheme() {
        assertThat(lastHttpStatus)
                .as("Supply before deregistration effective date must be accepted (2xx) — not yet implemented")
                .isBetween(200, 299);
    }

    @And("no error is raised for the supply date")
    public void noErrorRaisedForSupplyDate() {
        assertThat(lastHttpStatus)
                .as("No error expected for supply within valid registration window")
                .isBetween(200, 299);
    }

    /** FR-OSS-02-030 — re-registration after voluntary deregistration */
    @Given("a taxable person voluntarily deregistered from the EU scheme effective {localdate}")
    public void taxablePersonVoluntarilyDeregisteredEffective(LocalDate effectiveDate) {
        createActiveRegistration("EU");
        DeregistrationRequest request = new DeregistrationRequest(effectiveDate, "VOLUNTARY");
        postAction(currentRegistrationId, "deregister", request);
    }

    @When("they submit a new EU scheme registration notification on {localdate}")
    public void submitNewEuSchemeRegistrationNotificationOn(LocalDate date) {
        scheme = "EU";
        desiredStartDate = date;
        // Use a fresh registrant name to avoid duplicate-registrant guards
        registrantName = "Acme Services Ltd Reregistered";
        submitRegistration();
        secondRegistrationId = currentRegistrationId;
    }

    @Then("the registration is accepted")
    public void registrationIsAccepted() {
        assertThat(lastHttpStatus)
                .as("Re-registration after voluntary deregistration must be accepted — not yet implemented")
                .isBetween(200, 299);
    }

    @And("no exclusion flag or re-entry block is applied based on the prior deregistration")
    public void noExclusionFlagOrReEntryBlock() {
        assertThat(body().reRegistrationBlockUntil())
                .as("No re-registration block must be set after voluntary deregistration — not yet implemented")
                .isNull();
    }

    // =========================================================================
    // ── Feature: Forced exclusion and deregistration ──────────────────────────
    // =========================================================================

    /** FR-OSS-02-031 — criterion 1: cessation notification */
    @When("they notify Skatteforvaltningen on {localdate} that they no longer make eligible supplies")
    public void notifyNoLongerMakesEligibleSuppliesOn(LocalDate date) {
        exclusionDecisionDate = date;
        Map<String, Object> body = new HashMap<>();
        body.put("cessationDate", date.toString());
        body.put("criterion", "CESSATION_NOTIFICATION");
        postActionRaw(currentRegistrationId, "notify-cessation", body);
        fetchRegistration(currentRegistrationId);
    }

    @Then("the system initiates forced exclusion for criterion {string}")
    public void systemInitiatesForcedExclusionForCriterion(String criterion) {
        assertThat(body().exclusionCriterion())
                .as("Exclusion criterion must be [%s] — forced exclusion not yet implemented", criterion)
                .isEqualTo(criterion);
    }

    @And("the exclusion decision is sent electronically on {localdate}")
    public void exclusionDecisionSentElectronicallyOn(LocalDate date) {
        assertThat(body().exclusionDecisionDate())
                .as("Exclusion decision date must be [%s] — not yet implemented", date)
                .isEqualTo(date);
    }

    @And("the exclusion effective date is {localdate}")
    public void exclusionEffectiveDateIs(LocalDate expected) {
        assertThat(body().exclusionEffectiveDate())
                .as("Exclusion effective date must be [%s] — not yet implemented", expected)
                .isEqualTo(expected);
    }

    /** FR-OSS-02-032 — criterion 2: presumed cessation */
    @Given("a taxable person registered under the Non-EU scheme")
    public void taxablePersonRegisteredUnderNonEuScheme() {
        createActiveRegistration("NON_EU");
    }

    @And("they have reported no eligible supplies in any consumption member state since {localdate}")
    public void reportedNoEligibleSuppliesSince(LocalDate date) {
        noSuppliesSince = date;
    }

    @When("the system evaluates the registration on {localdate}")
    public void systemEvaluatesRegistrationOn(LocalDate date) {
        evaluationDate = date;
        Map<String, Object> body = new HashMap<>();
        body.put("evaluationDate", date.toString());
        body.put("noSuppliesSince", noSuppliesSince != null ? noSuppliesSince.toString() : null);
        postActionRaw(currentRegistrationId, "evaluate-cessation", body);
        fetchRegistration(currentRegistrationId);
    }

    @Then("the system detects presumed cessation after 2 full calendar years")
    public void systemDetectsPresumedCessation() {
        assertThat(body().exclusionCriterion())
                .as("Presumed cessation after 2 years must be detected — ML § 66j stk. 1 nr. 2 not yet implemented")
                .isEqualTo("PRESUMED_CESSATION");
    }

    @And("the exclusion effective date is the first day of the quarter following the decision date")
    public void exclusionEffectiveDateIsFirstDayOfQuarterFollowingDecision() {
        LocalDate decision = evaluationDate;
        // First day of next quarter
        int nextQuarterFirstMonth = ((decision.getMonthValue() - 1) / 3 + 1) * 3 + 1;
        int nextQuarterYear = decision.getYear();
        if (nextQuarterFirstMonth > 12) {
            nextQuarterFirstMonth -= 12;
            nextQuarterYear++;
        }
        LocalDate expectedExclusionDate = LocalDate.of(nextQuarterYear, nextQuarterFirstMonth, 1);
        assertThat(body().exclusionEffectiveDate())
                .as("Exclusion effective date must be first day of next quarter [%s] — not yet implemented",
                        expectedExclusionDate)
                .isEqualTo(expectedExclusionDate);
    }

    /** FR-OSS-02-032 — no exclusion before 2-year threshold */
    @Given("a taxable person registered under the EU scheme")
    public void taxablePersonRegisteredUnderEuScheme() {
        createActiveRegistration("EU");
    }

    @And("they have reported no eligible supplies since {localdate}")
    public void reportedNoEligibleSuppliesSinceEu(LocalDate date) {
        noSuppliesSince = date;
    }

    @Then("no presumed cessation exclusion is initiated")
    public void noPresumedCessationExclusionInitiated() {
        assertThat(body().exclusionCriterion())
                .as("No exclusion criterion must be set (threshold not reached) — not yet implemented")
                .isNull();
    }

    @And("the registration remains ACTIVE")
    public void registrationRemainsActive() {
        assertThat(body().status())
                .as("Registration status must remain ACTIVE — premature exclusion check not yet implemented")
                .isEqualTo("ACTIVE");
    }

    /** FR-OSS-02-033 — criterion 3: conditions no longer met */
    @Given("Skatteforvaltningen determines on {localdate} that a taxable person no longer meets Non-EU scheme conditions")
    public void skatteforvaltningenDeterminesConditionsNoLongerMet(LocalDate decisionDate) {
        exclusionDecisionDate = decisionDate;
        exclusionCriterion = "CONDITIONS_NOT_MET";
        createActiveRegistration("NON_EU");
    }

    @When("Skatteforvaltningen records the exclusion decision")
    public void skatteforvaltningenRecordsExclusionDecision() {
        // COMPILE-TIME FAILURE: ExclusionRequest record does not exist yet
        ExclusionRequest request = new ExclusionRequest(exclusionCriterion, exclusionDecisionDate.plusMonths(1).withDayOfMonth(1));
        postAction(currentRegistrationId, "exclude", request);
        fetchRegistration(currentRegistrationId);
    }

    @And("the taxable person is notified electronically on {localdate}")
    public void taxablePersonNotifiedElectronicallyOn(LocalDate date) {
        assertThat(body().exclusionDecisionDate())
                .as("Exclusion notification date must be [%s] — notification logic not yet implemented", date)
                .isEqualTo(date);
    }

    /** FR-OSS-02-034 — criterion 4: persistent non-compliance */
    @Given("Skatteforvaltningen determines on {localdate} that a taxable person has persistently failed to comply with EU scheme rules")
    public void skatteforvaltningenDeterminesPersistentNonCompliance(LocalDate decisionDate) {
        exclusionDecisionDate = decisionDate;
        exclusionCriterion = "PERSISTENT_NON_COMPLIANCE";
        createActiveRegistration("EU");
    }

    @When("the forced exclusion decision for {string} is recorded")
    public void forcedExclusionDecisionRecorded(String criterion) {
        ExclusionRequest request = new ExclusionRequest(criterion, exclusionDecisionDate.plusMonths(1).withDayOfMonth(1));
        postAction(currentRegistrationId, "exclude", request);
        fetchRegistration(currentRegistrationId);
    }

    @And("a re-registration block is set from {localdate} to {localdate}")
    public void reRegistrationBlockSetFromTo(LocalDate blockFrom, LocalDate blockTo) {
        assertThat(body().reRegistrationBlockUntil())
                .as("Re-registration block end must be [%s] — 2-year ban not yet implemented", blockTo)
                .isEqualTo(blockTo);
    }

    @And("any re-registration attempt before {localdate} is rejected with reason {string}")
    public void reRegistrationAttemptBeforeRejected(LocalDate blockUntil, String reason) {
        // Attempt a new registration — it must be rejected because the ban is active
        scheme = "EU";
        registrantName = "Acme Services Ltd ReAttempt";
        desiredStartDate = blockUntil.minusMonths(3);
        submitRegistration();
        assertThat(lastHttpStatus)
                .as("Re-registration before ban expiry [%s] must be rejected (4xx) — ban not yet implemented", blockUntil)
                .isBetween(400, 499);
        assertThat(lastErrorBody != null ? lastErrorBody : "")
                .as("Rejection body must reference reason keyword from [%s]", reason)
                .containsIgnoringCase("EXCLUDED");
    }

    /** FR-OSS-02-035 — criterion 1 does NOT impose 2-year ban */
    @Given("a taxable person is excluded for criterion {string} effective {localdate}")
    public void taxablePersonExcludedForCriterionEffective(String criterion, LocalDate effectiveDate) {
        createActiveRegistration("EU");
        ExclusionRequest request = new ExclusionRequest(criterion, effectiveDate);
        postAction(currentRegistrationId, "exclude", request);
    }

    @When("they attempt to re-register on {localdate}")
    public void attemptReRegisterOn(LocalDate date) {
        reRegistrationDate = date;
        scheme = "EU";
        registrantName = "Acme Services Ltd Regained";
        desiredStartDate = date;
        submitRegistration();
    }

    @Then("no 2-year block is applied")
    public void no2YearBlockApplied() {
        assertThat(lastHttpStatus)
                .as("Re-registration after cessation-notification exclusion must succeed — ban logic not yet implemented")
                .isBetween(200, 299);
        assertThat(body().reRegistrationBlockUntil())
                .as("No 2-year block must be set for cessation-notification criterion")
                .isNull();
    }

    /** FR-OSS-02-036 — establishment change → exclusion effective from move date */
    @Given("their home establishment moved from Denmark to France on {localdate}")
    public void homeEstablishmentMovedFromDenmarkToFranceOn(LocalDate date) {
        establishmentChangeDate = date;
    }

    @When("Skatteforvaltningen records forced exclusion due to the establishment change")
    public void recordsForcedExclusionDueToEstablishmentChange() {
        ExclusionRequest request = new ExclusionRequest("CONDITIONS_NOT_MET", establishmentChangeDate);
        postAction(currentRegistrationId, "exclude", request);
        fetchRegistration(currentRegistrationId);
    }

    @And("the standard next-quarter rule is not applied")
    public void standardNextQuarterRuleNotApplied() {
        // The exclusion effective date equals the establishment change date (not next quarter).
        assertThat(body().exclusionEffectiveDate())
                .as("Effective date must equal establishment change date, not next quarter — not yet implemented")
                .isEqualTo(establishmentChangeDate);
    }

    /** FR-OSS-02-036 — unauthorised exclusion actor */
    @When("an actor other than Skatteforvaltningen attempts to trigger a forced exclusion")
    public void unauthorisedActorAttemptsExclusion() {
        unauthorisedActor = true;
        // Simulate call without CASEWORKER role header (demo profile should still gate this)
        Map<String, Object> body = new HashMap<>();
        body.put("criterion", "CESSATION_NOTIFICATION");
        body.put("unauthorised", true);
        postActionRaw(currentRegistrationId, "exclude-unauthorised", body);
    }

    @Then("the system rejects the action with reason {string}")
    public void systemRejectsActionWithReason(String reason) {
        assertThat(lastHttpStatus)
                .as("Unauthorised exclusion must be rejected (4xx/5xx) — RBAC not yet implemented")
                .isGreaterThanOrEqualTo(400);
        // Also verify the error body carries the rejection reason token
        // e.g. reason = "UNAUTHORISED_EXCLUSION_ACTOR"
        String body = lastErrorBody != null ? lastErrorBody : "";
        String reasonKeyword = reason.split("_")[0]; // e.g. "UNAUTHORISED"
        assertThat(body)
                .as("Error body must reference reason [%s] — rejection reason propagation not yet implemented", reason)
                .containsIgnoringCase(reasonKeyword);
    }

    /** Post-exclusion VAT obligations */
    @Given("a taxable person has been excluded from the EU scheme effective {localdate}")
    public void taxablePersonExcludedEffective(LocalDate effectiveDate) {
        createActiveRegistration("EU");
        ExclusionRequest request = new ExclusionRequest("CESSATION_NOTIFICATION", effectiveDate);
        postAction(currentRegistrationId, "exclude", request);
    }

    @When("they make an eligible supply in Germany on {localdate}")
    public void makeEligibleSupplyInGermanyOn(LocalDate date) {
        supplyDate = date;
        supplyCountry = "DE";
        Map<String, Object> body = new HashMap<>();
        body.put("supplyDate", date.toString());
        body.put("supplyCountry", "DE");
        body.put("registrationId", currentRegistrationId.toString());
        postActionRaw(currentRegistrationId, "record-supply", body);
    }

    @Then("the system does not accept the supply under the EU scheme registration")
    public void systemDoesNotAcceptSupplyUnderEuScheme() {
        assertThat(lastHttpStatus)
                .as("Supply after exclusion must be rejected (4xx) — exclusion gate not yet implemented")
                .isBetween(400, 499);
    }

    @And("the system records that the taxable person must settle German VAT directly with German tax authorities")
    public void systemRecordsMustSettleVatDirectly() {
        assertThat(lastErrorBody != null ? lastErrorBody : "")
                .as("Response must indicate direct settlement obligation — not yet implemented")
                .containsIgnoringCase("SETTLE");
    }

    // =========================================================================
    // ── Feature: Scheme switching ─────────────────────────────────────────────
    // =========================================================================

    /** FR-OSS-02-037 — EU → Non-EU switch */
    @And("their last EU fixed establishment closes on {localdate}")
    public void lastEuFixedEstablishmentClosesOn(LocalDate date) {
        establishmentChangeDate = date;
        qualifiesForNonEu = true;
    }

    @And("they now qualify for the Non-EU scheme")
    public void theyNowQualifyForNonEuScheme() {
        qualifiesForNonEu = true;
    }

    @When("the system processes the establishment change effective {localdate}")
    public void systemProcessesEstablishmentChangeEffective(LocalDate date) {
        Map<String, Object> body = new HashMap<>();
        body.put("changeDate", date.toString());
        body.put("changeType", "EU_ESTABLISHMENT_CLOSED");
        body.put("newScheme", "NON_EU");
        postActionRaw(currentRegistrationId, "process-establishment-change", body);
        fetchRegistration(currentRegistrationId);
    }

    @Then("exclusion from the EU scheme is effective {localdate}")
    public void exclusionFromEuSchemeEffective(LocalDate expected) {
        assertThat(body().exclusionEffectiveDate())
                .as("EU scheme exclusion effective date must be [%s] — scheme switch not yet implemented", expected)
                .isEqualTo(expected);
    }

    @And("the Non-EU scheme registration becomes effective {localdate}")
    public void nonEuSchemeRegistrationBecomesEffective(LocalDate expected) {
        // OQ-5: assert final state via GET — check a second registration exists
        assertThat(body().newSchemeEffectiveDate())
                .as("Non-EU scheme new effective date must be [%s] — scheme switch not yet implemented", expected)
                .isEqualTo(expected);
    }

    @And("there is no gap period between the two scheme registrations")
    public void noGapPeriodBetweenSchemeRegistrations() {
        assertThat(body().exclusionEffectiveDate())
                .as("Exclusion and new registration dates must match (no gap) — not yet implemented")
                .isEqualTo(body().newSchemeEffectiveDate());
    }

    /** FR-OSS-02-037 — Non-EU → EU switch */
    @And("they establish a fixed place of business in Denmark on {localdate}")
    public void establishFixedPlaceOfBusinessInDenmarkOn(LocalDate date) {
        newEstablishmentDate = date;
    }

    @When("the system processes the new establishment effective {localdate}")
    public void systemProcessesNewEstablishmentEffective(LocalDate date) {
        Map<String, Object> body = new HashMap<>();
        body.put("changeDate", date.toString());
        body.put("changeType", "NEW_EU_ESTABLISHMENT");
        body.put("newScheme", "EU");
        postActionRaw(currentRegistrationId, "process-establishment-change", body);
        fetchRegistration(currentRegistrationId);
    }

    @Then("exclusion from the Non-EU scheme is effective {localdate}")
    public void exclusionFromNonEuSchemeEffective(LocalDate expected) {
        assertThat(body().exclusionEffectiveDate())
                .as("Non-EU scheme exclusion effective date must be [%s] — scheme switch not yet implemented", expected)
                .isEqualTo(expected);
    }

    @And("a new EU scheme registration may commence from {localdate}")
    public void newEuSchemeRegistrationMayCommenceFrom(LocalDate expected) {
        assertThat(body().newSchemeEffectiveDate())
                .as("New EU scheme effective date must be [%s] — scheme switch not yet implemented", expected)
                .isEqualTo(expected);
    }

    // =========================================================================
    // ── Feature: Transitional provision for pre-July-2021 registrations ───────
    // =========================================================================

    /** FR-OSS-02-038 */
    @Given("a taxable person was registered in the Non-EU scheme before {localdate}")
    public void registeredInNonEuSchemeBeforeDate(LocalDate date) {
        preJulyRegistrationDate = date;
        // For integration test purposes: create an ACTIVE registration and flag it
        // as pre-2021 via the request metadata field
        scheme = "NON_EU";
        desiredStartDate = date.minusDays(30);
        submitRegistration();
    }

    @And("they have not submitted an identification update under the new rules")
    public void haveNotSubmittedIdentificationUpdate() {
        // World-state flag; actual enforcement asserted in Then step
    }

    @When("the current date is {localdate}")
    public void currentDateIs(LocalDate date) {
        systemCurrentDate = date;
        Map<String, Object> body = new HashMap<>();
        body.put("systemDate", date.toString());
        body.put("registrationId", currentRegistrationId.toString());
        postActionRaw(currentRegistrationId, "evaluate-transitional", body);
        fetchRegistration(currentRegistrationId);
    }

    @Then("the registration is flagged as {string}")
    public void registrationFlaggedAs(String flag) {
        assertThat(body().complianceFlags())
                .as("Compliance flag [%s] must be present — transitional check not yet implemented", flag)
                .contains(flag);
    }

    @Then("the registration is NOT flagged as {string}")
    public void registrationNotFlaggedAs(String flag) {
        List<String> flags = body().complianceFlags();
        if (flags != null) {
            assertThat(flags)
                    .as("Flag [%s] must NOT be present before 1 April 2022 — not yet implemented", flag)
                    .doesNotContain(flag);
        }
        // null or empty list also satisfies the requirement
    }

    @Given("a taxable person's registration is flagged as {string}")
    public void taxablePersonRegistrationFlaggedAs(String flag) {
        createActiveRegistration("NON_EU");
        // Simulate setting the flag via evaluation endpoint
        Map<String, Object> body = new HashMap<>();
        body.put("systemDate", LocalDate.of(2022, 4, 2).toString());
        body.put("registrationId", currentRegistrationId.toString());
        postActionRaw(currentRegistrationId, "evaluate-transitional", body);
        fetchRegistration(currentRegistrationId);
    }

    @When("they submit a complete identification update")
    public void submitCompleteIdentificationUpdate() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", registrantName);
        body.put("address", postalAddress);
        body.put("email", email);
        body.put("phoneNumber", phoneNumber);
        body.put("homeCountryTaxNumber", homeCountryTaxNumber);
        body.put("bankDetails", bankDetails);
        body.put("updateType", "TRANSITIONAL_IDENTIFICATION_UPDATE");
        postActionRaw(currentRegistrationId, "submit-identification-update", body);
        fetchRegistration(currentRegistrationId);
    }

    @Then("the flag is cleared")
    public void flagIsCleared() {
        List<String> flags = body().complianceFlags();
        if (flags != null) {
            assertThat(flags)
                    .as("TRANSITIONAL_UPDATE_OVERDUE flag must be cleared after update — not yet implemented")
                    .doesNotContain("TRANSITIONAL_UPDATE_OVERDUE");
        }
    }

    @And("the registration record shows the update date")
    public void registrationRecordShowsUpdateDate() {
        assertThat(body().lastIdentificationUpdateDate())
                .as("Last identification update date must be present — not yet implemented")
                .isNotNull();
    }

    @When("the system attempts to open a new quarterly return period for the taxable person")
    public void systemAttemptsOpenNewQuarterlyReturnPeriod() {
        Map<String, Object> body = new HashMap<>();
        body.put("registrationId", currentRegistrationId.toString());
        body.put("returnPeriodStart", LocalDate.now().withDayOfMonth(1).toString());
        postActionRaw(currentRegistrationId, "open-return-period", body);
    }

    @Then("the return period is not opened")
    public void returnPeriodIsNotOpened() {
        assertThat(lastHttpStatus)
                .as("Return period must not be opened for transitional-overdue registrant (4xx) — not yet implemented")
                .isBetween(400, 499);
    }

    @And("the block reason is {string}")
    public void blockReasonIs(String reason) {
        assertThat(lastErrorBody != null ? lastErrorBody : "")
                .as("Block reason must contain [%s] — not yet implemented", reason)
                .containsIgnoringCase(reason.replace("_", " ").split(":")[0]);
    }

    @Given("a taxable person was registered in the EU scheme before {localdate}")
    public void registeredInEuSchemeBeforeDate(LocalDate date) {
        preJulyRegistrationDate = date;
        scheme = "EU";
        homeCountry = "DK";
        homeCountryTaxNumber = null;
        desiredStartDate = date.minusDays(30);
        submitRegistration();
    }

    @And("they have not submitted an identification update")
    public void haveNotSubmittedIdentificationUpdateEu() {
        // World-state flag; no action required — assertion handled by Then step
    }
}
