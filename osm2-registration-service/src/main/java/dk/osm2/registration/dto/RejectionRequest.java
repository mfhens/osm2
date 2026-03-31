package dk.osm2.registration.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for rejecting a pending registration.
 *
 * <p>Legal basis: ML § 66b stk. 2
 * <p>Petition: OSS-02 — FR-OSS-02-013
 */
public record RejectionRequest(

        /** Reason for rejecting the registration (mandatory). */
        @NotBlank String reason

) {}
