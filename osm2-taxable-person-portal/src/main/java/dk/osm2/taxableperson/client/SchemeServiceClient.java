package dk.osm2.taxableperson.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.osm2.taxableperson.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * OkHttp-based client for the OSM2 scheme-service.
 *
 * <p>Calls {@code POST /api/v1/schemes/classify} to determine which OSS VAT scheme applies.
 *
 * <p>Classification is functionally idempotent (same inputs always produce the same scheme
 * result), so the call is protected by both a circuit breaker and a retry per ADR-0026.
 */
@Slf4j
@Component
public class SchemeServiceClient {

  private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

  private final String baseUrl;
  private final OkHttpClient http;
  private final ObjectMapper objectMapper;

  public SchemeServiceClient(
      @Value("${osm2.services.scheme-service.base-url}") String baseUrl,
      OkHttpClient http,
      ObjectMapper objectMapper) {
    this.baseUrl = baseUrl;
    this.http = http;
    this.objectMapper = objectMapper;
  }

  /**
   * Classify the taxable person's supply into an OSS scheme.
   *
   * <p>Idempotent — protected by circuit breaker and retry with exponential backoff.
   *
   * @param request classification inputs from wizard step 1
   * @return classification result from the scheme-service
   * @throws ServiceUnavailableException if the service is unreachable or returns non-2xx
   */
  @CircuitBreaker(name = "schemeService", fallbackMethod = "classifyFallback")
  @Retry(name = "schemeService")
  public SchemeClassificationResponse classify(SchemeClassificationRequest request) {
    try {
      String json = objectMapper.writeValueAsString(request);
      Request httpRequest =
          new Request.Builder()
              .url(baseUrl + "/api/v1/schemes/classify")
              .post(RequestBody.create(json, JSON))
              .build();

      try (Response response = http.newCall(httpRequest).execute()) {
        if (!response.isSuccessful()) {
          log.error("scheme-service classify returned HTTP {}", response.code());
          throw new ServiceUnavailableException(
              "scheme-service returned HTTP " + response.code());
        }
        String body = response.body().string();
        return objectMapper.readValue(body, SchemeClassificationResponse.class);
      }
    } catch (IOException e) {
      log.error("scheme-service classify failed: {}", e.getMessage(), e);
      throw new ServiceUnavailableException("scheme-service unreachable", e);
    }
  }

  // ---------------------------------------------------------------------------
  // Fallback methods [ADR-0026]
  // ---------------------------------------------------------------------------

  private SchemeClassificationResponse classifyFallback(SchemeClassificationRequest request, Throwable t) {
    log.error("scheme-service circuit open for classify: {}", t.getMessage());
    throw new ServiceUnavailableException("scheme-service unavailable (circuit open)", t);
  }
}
