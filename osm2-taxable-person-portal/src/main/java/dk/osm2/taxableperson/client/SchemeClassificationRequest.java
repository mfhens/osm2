package dk.osm2.taxableperson.client;

import java.math.BigDecimal;

/**
 * Simplified classification request sent to the scheme-service {@code POST /api/v1/schemes/classify}.
 *
 * <p>Only the fields required by the wizard's step-1 form are included. The scheme-service accepts
 * the full {@code SchemeClassificationRequest} but ignores unknown fields, so this subset is safe.
 */
public record SchemeClassificationRequest(
    String supplyType,
    String homeCountry,
    String consumptionMemberState,
    Boolean hasEuSeatOfEconomicActivity,
    String selectedIdentificationMemberState,
    BigDecimal shipmentValue) {}
