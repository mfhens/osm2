package dk.osm2.common.arch;

/**
 * Canonical root package names for every osm2 service.
 *
 * <p>Used by {@link SharedArchitectureRules} to parameterise cross-service import rules.
 * When a new service is added, register its root package here and add an ArchitectureTest
 * in that module.
 */
public final class ServicePackages {

    public static final String SCHEME              = "dk.osm2.scheme";
    public static final String REGISTRATION        = "dk.osm2.registration";
    public static final String RETURNS             = "dk.osm2.returns";
    public static final String PAYMENT             = "dk.osm2.payment";
    public static final String RECORDS             = "dk.osm2.records";
    public static final String TAXABLE_PERSON_PORTAL = "dk.osm2.taxableperson";
    public static final String AUTHORITY_PORTAL    = "dk.osm2.authority";
    public static final String COMMON              = "dk.osm2.common";

    /** All runtime service roots — used to build the cross-service import exclusion lists. */
    public static final String[] ALL_SERVICE_ROOTS = {
        SCHEME, REGISTRATION, RETURNS, PAYMENT, RECORDS,
        TAXABLE_PERSON_PORTAL, AUTHORITY_PORTAL
    };

    private ServicePackages() {}
}
