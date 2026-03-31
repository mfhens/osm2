package dk.osm2.registration.dto;

/**
 * Request DTO for approving a pending registration and assigning a VAT number.
 *
 * <p>Legal basis: ML § 66b stk. 2
 * <p>Petition: OSS-02 — FR-OSS-02-013
 *
 * <p>OQ-1 assumption: VAT number is a non-null String; placeholder is
 * {@code "DK" + 8-char UUID segment} when the caller does not provide one.
 */
public record ApprovalRequest(

        /** The OSS VAT number to assign. May be null to trigger auto-generation. */
        String vatNumber

) {}
