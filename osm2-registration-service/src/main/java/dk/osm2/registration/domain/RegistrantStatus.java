package dk.osm2.registration.domain;

/**
 * Lifecycle status of a scheme registration.
 *
 * <p>Legal basis: ML §§ 66a–66j
 * <p>Petition: OSS-02
 */
public enum RegistrantStatus {

    /** Registration submitted; awaiting VAT number assignment (ML § 66b stk. 2). */
    PENDING_VAT_NUMBER,

    /** Registration approved and VAT number assigned. Active in scheme. */
    ACTIVE,

    /** Registration deregistered (voluntary or forced). */
    DEREGISTERED,

    /** Registration excluded by Skatteforvaltningen (ML § 66j). */
    EXCLUDED,

    /** Taxable person has submitted cessation notification (ML § 66h). */
    CESSATION_NOTIFIED
}
