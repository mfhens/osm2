package dk.osm2.taxableperson.client;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Response from the registration-service for a single registration resource.
 *
 * <p>Field names match the JSON produced by {@code RegistrationController} in the
 * registration-service (OSS-02 spec).
 */
public record RegistrationResponse(
    UUID registrationId,
    UUID registrantId,
    String status,
    LocalDate effectiveDate,
    String vatNumber,
    String schemeType,
    String legalBasis) {}
