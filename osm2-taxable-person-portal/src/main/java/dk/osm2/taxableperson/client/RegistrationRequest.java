package dk.osm2.taxableperson.client;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request body for {@code POST /api/v1/registrations} on the registration-service.
 *
 * <p>Field names match the registration-service API contract (OSS-02 spec).
 */
public record RegistrationRequest(
    String registrantName,
    String scheme,
    String homeCountry,
    String homeCountryTaxNumber,
    String postalAddress,
    String email,
    String phoneNumber,
    String bankDetails,
    String identificationMemberState,
    LocalDate firstDeliveryDate,
    UUID registrantId) {}
