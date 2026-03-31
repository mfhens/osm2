package dk.osm2.scheme.service;

import dk.osm2.scheme.dto.SchemeClassificationRequest;
import dk.osm2.scheme.dto.SchemeClassificationRequest.SupplyType;
import dk.osm2.scheme.dto.SchemeClassificationResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;

/**
 * Stateless scheme eligibility classification service.
 *
 * <p>Pre-validation rules run in Java (V-01, V-02) for structural completeness. Drools rules
 * (FR-01 through FR-08) run inside a per-request KieSession and determine the applicable scheme,
 * identification member state, consumption member state, and legal citations.
 *
 * <p>{@link ClassificationResult} acts as a mutable Drools fact: rules modify its public fields
 * and call {@code update()} so that downstream rules see the updated values.
 *
 * <p>Petition: OSS-01 / ML §§ 66, 66a, 66d, 66m / MSD artikler 358, 358a, 369a, 369l
 */
@Service
@RequiredArgsConstructor
public class SchemeClassificationService {

    private final KieContainer kieContainer;

    /**
     * Classifies the taxable person into an OSS scheme.
     *
     * @param request classification inputs (stateless — no DB reads/writes)
     * @return the classification result with scheme code, IMS, CMS, and legal basis
     */
    public SchemeClassificationResult classify(SchemeClassificationRequest request) {

        // ------------------------------------------------------------------
        // V-01 — missing establishment info (AC-20)
        // Only fires when supplyType is not IMPORT_GOODS (Import can proceed
        // without explicit establishment info when eligibility is the question).
        // ------------------------------------------------------------------
        if (request.getHasEuSeatOfEconomicActivity() == null
                && request.getHasFixedEstablishmentInEu() == null
                && request.getEnrolledScheme() == null
                && request.getSupplyType() != SupplyType.IMPORT_GOODS) {
            return new SchemeClassificationResult(
                    "INSUFFICIENT_INFORMATION",
                    null,
                    null,
                    null,
                    List.of(),
                    "Utilstrækkelige oplysninger — etableringsland eller etableringstype mangler",
                    List.of());
        }

        // ------------------------------------------------------------------
        // V-02 — Import goods without shipment value (AC-21)
        // Only fires when enrolledScheme is null (eligibility check, not
        // ID/consumption MS determination for an already-enrolled person).
        // ------------------------------------------------------------------
        if (request.getSupplyType() == SupplyType.IMPORT_GOODS
                && request.getShipmentValue() == null
                && request.getEnrolledScheme() == null) {
            return new SchemeClassificationResult(
                    "INSUFFICIENT_INFORMATION",
                    null,
                    null,
                    null,
                    List.of(),
                    "Utilstrækkelige oplysninger — reel forsendelsesværdi skal angives",
                    List.of());
        }

    // -----------------------------------------------------------------------
    // Drools — FR-01 through FR-08
    // -----------------------------------------------------------------------
        ClassificationResult result = new ClassificationResult();
        KieSession session = kieContainer.newKieSession();
        try {
            session.insert(request);
            session.insert(result);
            session.fireAllRules();
        } finally {
            session.dispose();
        }

        return new SchemeClassificationResult(
                result.status,
                result.scheme,
                result.identificationMemberState,
                result.consumptionMemberState,
                result.legalBasis,
                result.message,
                result.applicableRules);
    }
}
