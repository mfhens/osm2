package dk.osm2.registration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Request DTO for recording a forced exclusion from an OSS scheme.
 *
 * <p>Legal basis: ML § 66j stk. 1
 * <p>Petition: OSS-02 — FR-OSS-02-031 through FR-OSS-02-035
 *
 * <p>Ref: RegistrationSteps — constructor call: new ExclusionRequest(criterion, effectiveDate, decisionDate)
 */
public record ExclusionRequest(

        /** Exclusion criterion (e.g., "PERSISTENT_NON_COMPLIANCE", "CONDITIONS_NOT_MET"). */
        @NotBlank String criterion,

        /** Effective date of the exclusion (valid-time, ML § 66j stk. 2). */
        @NotNull LocalDate effectiveDate,

        /**
         * Date the exclusion decision was issued and communicated to the taxable person.
         * Null means "use today's date" as the decision date.
         */
        LocalDate decisionDate

) {}
