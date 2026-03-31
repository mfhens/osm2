package dk.osm2.registration.exception;

import dk.osm2.registration.domain.SchemeType;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Thrown when a re-registration is attempted while a 2-year exclusion ban is active.
 *
 * <p>Maps to HTTP 422 Unprocessable Entity.
 * <p>Legal basis: ML § 66j stk. 2 (2-year re-registration ban after PERSISTENT_NON_COMPLIANCE)
 * <p>Petition: OSS-02 — FR-OSS-02-034
 */
public class ExclusionBanActiveException extends RuntimeException {

    public ExclusionBanActiveException(UUID registrantId, SchemeType schemeType, LocalDate banLiftedAt) {
        super("Re-registration blocked for registrant " + registrantId
              + " in scheme " + schemeType
              + " until " + banLiftedAt
              + " — EXCLUDED: exclusion ban is active (ML § 66j stk. 2)");
    }

    public ExclusionBanActiveException(String message) {
        super(message);
    }
}
