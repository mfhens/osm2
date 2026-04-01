package dk.osm2.taxableperson.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.osm2.taxableperson.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * OkHttp-based client for the OSM2 registration-service.
 *
 * <p>Forwards the OAuth2 bearer token obtained from the current security context to the
 * registration-service so that downstream authorization checks use the same identity.
 * In demo mode no token is present and the Authorization header is omitted.
 *
 * <p>All public methods are protected with Resilience4j circuit breakers per ADR-0026.
 * Idempotent reads additionally carry a {@code @Retry} annotation. Non-idempotent
 * mutations (POST/PUT) carry a circuit breaker only to avoid duplicate writes.
 */
@Slf4j
@Component
public class RegistrationServiceClient {

  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

  private final String baseUrl;
  private final OkHttpClient http;
  private final ObjectMapper objectMapper;
  private final OAuth2AuthorizedClientService authorizedClientService;

  public RegistrationServiceClient(
      @Value("${osm2.services.registration-service.base-url}") String baseUrl,
      OkHttpClient http,
      ObjectMapper objectMapper,
      OAuth2AuthorizedClientService authorizedClientService) {
    this.baseUrl = baseUrl;
    this.http = http;
    this.objectMapper = objectMapper;
    this.authorizedClientService = authorizedClientService;
  }

  /**
   * Submit a new registration to the registration-service.
   *
   * <p>Non-idempotent — protected by circuit breaker only (no retry) to prevent duplicate writes.
   *
   * @param request the registration payload built from the wizard session
   * @return the created registration resource
   * @throws ServiceUnavailableException on network error or non-2xx response
   */
  @CircuitBreaker(name = "registrationService", fallbackMethod = "submitRegistrationFallback")
  public RegistrationResponse submitRegistration(RegistrationRequest request) {
    try {
      String json = objectMapper.writeValueAsString(request);
      Request httpRequest =
          new Request.Builder()
              .url(baseUrl + "/api/v1/registrations")
              .addHeader("Authorization", bearerToken())
              .post(RequestBody.create(json, JSON))
              .build();

      try (Response response = http.newCall(httpRequest).execute()) {
        if (!response.isSuccessful()) {
          log.error("registration-service POST /registrations returned HTTP {}", response.code());
          throw new ServiceUnavailableException(
              "registration-service returned HTTP " + response.code());
        }
        return objectMapper.readValue(response.body().string(), RegistrationResponse.class);
      }
    } catch (IOException e) {
      log.error("registration-service submitRegistration failed: {}", e.getMessage(), e);
      throw new ServiceUnavailableException("registration-service unreachable", e);
    }
  }

  /**
   * Retrieve a single registration by its ID.
   *
   * <p>Idempotent — protected by circuit breaker and retry with exponential backoff.
   *
   * @param id registration UUID
   * @return the registration resource
   * @throws ServiceUnavailableException on network error or non-2xx response
   */
  @CircuitBreaker(name = "registrationService", fallbackMethod = "getRegistrationFallback")
  @Retry(name = "registrationService")
  public RegistrationResponse getRegistration(UUID id) {
    try {
      Request httpRequest =
          new Request.Builder()
              .url(baseUrl + "/api/v1/registrations/" + id)
              .addHeader("Authorization", bearerToken())
              .get()
              .build();

      try (Response response = http.newCall(httpRequest).execute()) {
        if (!response.isSuccessful()) {
          log.error("registration-service GET /registrations/{} returned HTTP {}", id, response.code());
          throw new ServiceUnavailableException(
              "registration-service returned HTTP " + response.code());
        }
        return objectMapper.readValue(response.body().string(), RegistrationResponse.class);
      }
    } catch (IOException e) {
      log.error("registration-service getRegistration failed: {}", e.getMessage(), e);
      throw new ServiceUnavailableException("registration-service unreachable", e);
    }
  }

  /**
   * List all registrations belonging to the given registrant.
   *
   * <p>Idempotent — protected by circuit breaker and retry with exponential backoff.
   *
   * @param registrantId the registrant's UUID (sourced from OIDC {@code sub} claim)
   * @return list of registration resources (may be empty)
   * @throws ServiceUnavailableException on network error or non-2xx response
   */
  @CircuitBreaker(name = "registrationService", fallbackMethod = "listMyRegistrationsFallback")
  @Retry(name = "registrationService")
  public List<RegistrationResponse> listMyRegistrations(UUID registrantId) {
    try {
      Request httpRequest =
          new Request.Builder()
              .url(baseUrl + "/api/v1/registrations?registrantId=" + registrantId)
              .addHeader("Authorization", bearerToken())
              .get()
              .build();

      try (Response response = http.newCall(httpRequest).execute()) {
        if (!response.isSuccessful()) {
          log.error(
              "registration-service GET /registrations?registrantId={} returned HTTP {}",
              registrantId,
              response.code());
          throw new ServiceUnavailableException(
              "registration-service returned HTTP " + response.code());
        }
        return objectMapper.readValue(
            response.body().string(), new TypeReference<List<RegistrationResponse>>() {});
      }
    } catch (IOException e) {
      log.error("registration-service listMyRegistrations failed: {}", e.getMessage(), e);
      throw new ServiceUnavailableException("registration-service unreachable", e);
    }
  }

  // ---------------------------------------------------------------------------
  // Fallback methods [ADR-0026]
  // Called by Resilience4j when the circuit is open or all retries are exhausted.
  // ---------------------------------------------------------------------------

  private RegistrationResponse submitRegistrationFallback(RegistrationRequest request, Throwable t) {
    log.error("registration-service circuit open for submitRegistration: {}", t.getMessage());
    throw new ServiceUnavailableException("registration-service unavailable (circuit open)", t);
  }

  private RegistrationResponse getRegistrationFallback(UUID id, Throwable t) {
    log.error("registration-service circuit open for getRegistration({}): {}", id, t.getMessage());
    throw new ServiceUnavailableException("registration-service unavailable (circuit open)", t);
  }

  private List<RegistrationResponse> listMyRegistrationsFallback(UUID registrantId, Throwable t) {
    log.error("registration-service circuit open for listMyRegistrations({}): {}", registrantId, t.getMessage());
    throw new ServiceUnavailableException("registration-service unavailable (circuit open)", t);
  }

  // ---------------------------------------------------------------------------
  // Internal helpers
  // ---------------------------------------------------------------------------

  /**
   * Extracts the OAuth2 Bearer token from the current security context.
   *
   * <p>Returns an empty string in demo mode (no token present), which causes the Authorization
   * header to be sent as an empty string — downstream services in demo mode must accept this.
   */
  private String bearerToken() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof OAuth2AuthenticationToken oauthToken) {
      OAuth2AuthorizedClient client =
          authorizedClientService.loadAuthorizedClient(
              oauthToken.getAuthorizedClientRegistrationId(), oauthToken.getName());
      if (client != null && client.getAccessToken() != null) {
        return "Bearer " + client.getAccessToken().getTokenValue();
      }
    }
    return "";
  }
}

