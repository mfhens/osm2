package dk.osm2.registration.domain;

/**
 * Criteria for forced exclusion from an OSS scheme.
 *
 * <p>Legal basis: ML § 66j stk. 1
 * <p>Petition: OSS-02
 */
public enum ExclusionCriterion {

    /** Persistently failed to comply with EU VAT rules (ML § 66j stk. 1 nr. 4). */
    PERSISTENT_NON_COMPLIANCE,

    /** Serious irregularity detected (ML § 66j stk. 1 nr. 3). */
    SERIOUS_IRREGULARITY,

    /** Repeated failure to comply (ML § 66j stk. 1 nr. 4). */
    REPEATED_FAILURE_TO_COMPLY,

    /** Voluntary cessation — taxable person notified cessation (ML § 66h). */
    VOLUNTARY_CESSATION
}
