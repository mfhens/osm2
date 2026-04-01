package dk.osm2.authority;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import dk.osm2.common.arch.ServicePackages;
import dk.osm2.common.arch.SharedArchitectureRules;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Architecture rules for osm2-authority-portal.
 *
 * <p>R-06 is disabled: RegistrationServiceClient currently lacks
 * {@code @CircuitBreaker} annotations. Enable after ADR-0026 compliance is
 * implemented in the portal client classes.
 */
@AnalyzeClasses(packages = "dk.osm2.authority", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule r01NoPiiFieldsInNonRegistrationEntities =
            SharedArchitectureRules.NO_PII_FIELDS_IN_NON_REGISTRATION_ENTITIES;

    @ArchTest
    static final ArchRule r02EntitiesMustExtendAuditableEntity =
            SharedArchitectureRules.ENTITIES_MUST_EXTEND_AUDITABLE_ENTITY;

    @ArchTest
    static final ArchRule r03NoCrossServiceImports =
            SharedArchitectureRules.noCrossServiceImports(ServicePackages.AUTHORITY_PORTAL);

    @ArchTest
    static final ArchRule r04NoMessageBrokerImports =
            SharedArchitectureRules.NO_MESSAGE_BROKER_IMPORTS;

    @ArchTest
    static final Architectures.LayeredArchitecture r05LayeredArchitecture =
            SharedArchitectureRules.layeredArchitectureFor(ServicePackages.AUTHORITY_PORTAL);

    @ArchTest
    static final ArchRule r07ApiUrlPrefixConvention =
            SharedArchitectureRules.API_URL_PREFIX_CONVENTION;

    @ArchTest
    static final ArchRule r08aDomainMustNotDependOnSpringWeb =
            SharedArchitectureRules.DOMAIN_CLASSES_MUST_NOT_DEPEND_ON_SPRING_WEB;

    @ArchTest
    static final ArchRule r08bDomainMustNotHaveSpringStereotypes =
            SharedArchitectureRules.DOMAIN_CLASSES_MUST_NOT_HAVE_SPRING_STEREOTYPES;

    @ArchTest
    static final ArchRule r09NoEntityReturnedFromController =
            SharedArchitectureRules.NO_ENTITY_RETURNED_FROM_CONTROLLER;

    @ArchTest
    static final ArchRule r10ExceptionsInExceptionPackage =
            SharedArchitectureRules.EXCEPTIONS_IN_EXCEPTION_PACKAGE;

    // -------------------------------------------------------------------------
    // R-06 — DISABLED pending ADR-0026 compliance [ADR-0026]
    // -------------------------------------------------------------------------

    @Test
    @Disabled("R-06: Enable once RegistrationServiceClient methods carry"
            + " @CircuitBreaker/@Retry/@TimeLimiter annotations [ADR-0026]")
    void r06ClientMethodsMustHaveResilience() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ServicePackages.AUTHORITY_PORTAL);
        SharedArchitectureRules.CLIENT_METHODS_MUST_HAVE_RESILIENCE.check(classes);
    }
}
