package dk.osm2.registration.domain;

/**
 * OSS scheme types as defined in ADR-0031.
 *
 * <p>Constants are statutory — do not rename.
 * <p>Legal basis: ML §§ 66a (NON_EU), 66d (EU), 66m (IMPORT)
 * <p>Petition: OSS-02
 */
public enum SchemeType {

    /** Ikke-EU-ordningen — ML § 66a / MSD art. 358a. */
    NON_EU,

    /** EU-ordningen — ML § 66d / MSD art. 369a. */
    EU,

    /** Importordningen — ML § 66m / MSD art. 369l. */
    IMPORT
}
