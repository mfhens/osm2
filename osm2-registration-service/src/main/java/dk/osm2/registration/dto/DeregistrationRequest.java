package dk.osm2.registration.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Request DTO for voluntary deregistration from an OSS scheme.
 *
 * <p>Legal basis: ML § 66h
 * <p>Petition: OSS-02 — FR-OSS-02-027 through FR-OSS-02-030
 *
 * <p>Ref: RegistrationSteps — constructor call: new DeregistrationRequest(date, "VOLUNTARY")
 */
public record DeregistrationRequest(

        /** Requested effective date of deregistration (mandatory). */
        @NotNull LocalDate effectiveDate,

        /** Reason for deregistration (e.g., "VOLUNTARY", "CESSATION"). */
        String reason

) {}
