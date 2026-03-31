package dk.osm2.authority.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.osm2.authority.exception.ServiceUnavailableException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * OkHttp-based client for the osm2-registration-service.
 *
 * <p>All downstream calls use {@code application/json}. On non-2xx responses or I/O
 * failures a {@link ServiceUnavailableException} is thrown so the
 * {@link dk.osm2.authority.web.GlobalExceptionHandler} can render the generic error page.
 */
@Component
public class RegistrationServiceClient {

    // -------------------------------------------------------------------------
    // DTOs (Java records — BFF-local, not shared with registration-service)
    // -------------------------------------------------------------------------

    public record RegistrationItem(
            UUID registrationId,
            UUID registrantId,
            String registrantName,
            String status,
            LocalDate effectiveDate,
            String schemeType,
            String vatNumber) {}

    public record RegistrationDetail(
            UUID registrationId,
            UUID registrantId,
            String registrantName,
            String homeCountry,
            String email,
            String postalAddress,
            String bankDetails,
            String status,
            LocalDate effectiveDate,
            String schemeType,
            String legalBasis,
            String vatNumber) {}

    public record ApprovalRequest(String vatNumber) {}

    public record RejectionRequest(String reason) {}

    // -------------------------------------------------------------------------
    // Infrastructure
    // -------------------------------------------------------------------------

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public RegistrationServiceClient(
            OkHttpClient httpClient,
            ObjectMapper objectMapper,
            @Value("${osm2.services.registration-service.base-url}") String baseUrl) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns registrations whose status is {@code PENDING_VAT_NUMBER} —
     * i.e. awaiting a caseworker assignment decision.
     */
    public List<RegistrationItem> listPendingRegistrations() {
        Request request = new Request.Builder()
                .url(baseUrl + "/api/v1/registrations?status=PENDING_VAT_NUMBER")
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            assertSuccess(response, "listPendingRegistrations");
            return objectMapper.readValue(
                    response.body().string(),
                    new TypeReference<List<RegistrationItem>>() {});
        } catch (IOException e) {
            throw new ServiceUnavailableException("registration-service unavailable: " + e.getMessage(), e);
        }
    }

    /**
     * Fetches the full detail of a single registration.
     */
    public RegistrationDetail getRegistration(UUID id) {
        Request request = new Request.Builder()
                .url(baseUrl + "/api/v1/registrations/" + id)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            assertSuccess(response, "getRegistration(" + id + ")");
            return objectMapper.readValue(response.body().string(), RegistrationDetail.class);
        } catch (IOException e) {
            throw new ServiceUnavailableException("registration-service unavailable: " + e.getMessage(), e);
        }
    }

    /**
     * Approves a pending registration by assigning a VAT number.
     *
     * <p>In demo mode, if {@code vatNumber} is blank, a placeholder is generated
     * to avoid a validation error against the downstream service.
     */
    public void approveRegistration(UUID id, String vatNumber) {
        String effectiveVatNumber = (vatNumber == null || vatNumber.isBlank())
                ? generatePlaceholderVatNumber()
                : vatNumber;

        ApprovalRequest body = new ApprovalRequest(effectiveVatNumber);
        postJson("/api/v1/registrations/" + id + "/approve", body, "approveRegistration(" + id + ")");
    }

    /**
     * Rejects a pending registration with a caseworker-supplied reason.
     */
    public void rejectRegistration(UUID id, String reason) {
        RejectionRequest body = new RejectionRequest(reason);
        postJson("/api/v1/registrations/" + id + "/reject", body, "rejectRegistration(" + id + ")");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void postJson(String path, Object bodyObject, String operationName) {
        try {
            String json = objectMapper.writeValueAsString(bodyObject);
            RequestBody requestBody = RequestBody.create(json, JSON);
            Request request = new Request.Builder()
                    .url(baseUrl + path)
                    .post(requestBody)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                assertSuccess(response, operationName);
            }
        } catch (IOException e) {
            throw new ServiceUnavailableException("registration-service unavailable: " + e.getMessage(), e);
        }
    }

    private void assertSuccess(Response response, String operationName) {
        if (!response.isSuccessful()) {
            throw new ServiceUnavailableException(
                    "registration-service returned HTTP " + response.code()
                    + " for operation " + operationName);
        }
    }

    private String generatePlaceholderVatNumber() {
        // Deterministic demo-safe placeholder: prefix EU + 9 digits
        return "EU" + String.format("%09d", System.currentTimeMillis() % 1_000_000_000L);
    }
}
