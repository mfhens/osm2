package dk.osm2.scheme.web;

import dk.osm2.scheme.dto.SchemeClassificationRequest;
import dk.osm2.scheme.dto.SchemeClassificationResult;
import dk.osm2.scheme.service.SchemeClassificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for OSS scheme eligibility classification.
 *
 * <p>Exposes a single stateless endpoint: {@code POST /api/v1/schemes/classify}.
 *
 * <p>Security: provided by the keycloak-oauth2-starter {@code SecurityFilterChain}. Do <b>not</b>
 * add {@code @PreAuthorize} or a manual {@code SecurityFilterChain} here (ADR-0004).
 *
 * <p>Legal basis: ML §§ 66–66u / MSD artikler 358, 358a, 369a, 369l
 *
 * <p>Petition: OSS-01 — Juridisk grundlag, skemavurdering og definitioner
 */
@RestController
@RequestMapping("/api/v1/schemes")
@RequiredArgsConstructor
@Tag(
        name = "Scheme Classification",
        description =
                "OSS scheme eligibility classification — ML §§ 66–66u / MSD artikler 358–369l")
public class SchemeClassificationController {

    private final SchemeClassificationService classificationService;

    /**
     * Classify a taxable person into one of the three OSS special arrangements.
     *
     * <p>Determines whether the Non-EU scheme (ML § 66a / MSD art. 358a), EU scheme (ML § 66d /
     * MSD art. 369a), or Import scheme (ML § 66m / MSD art. 369l) applies, or whether no OSS
     * scheme is applicable. Classification is purely stateless — no database writes occur.
     *
     * <p>The result always carries at least one entry in {@code legalBasis} (FR-08 / AC-26).
     */
    @PostMapping("/classify")
    @Operation(
            summary = "Classify taxable person into OSS scheme",
            description =
                    "Stateless classification against the three OSS special arrangements. "
                            + "Returns the applicable scheme code (NON_EU | EU | IMPORT), "
                            + "identification member state, consumption member state, and "
                            + "legal basis citations (ML § + MSD artikel).")
    public ResponseEntity<SchemeClassificationResult> classify(
            @RequestBody SchemeClassificationRequest request) {
        return ResponseEntity.ok(classificationService.classify(request));
    }
}
