package dk.osm2.returns;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import dk.osm2.common.arch.ServicePackages;
import dk.osm2.common.arch.SharedArchitectureRules;

/**
 * Architecture rules for osm2-return-service.
 *
 * <p>This service is currently a stub (no source classes). All rules pass vacuously.
 * They will gate implementation from the first commit.
 *
 * <p>R-06 is included here as a placeholder comment — add it as a disabled @Test
 * once .client classes are introduced.
 *
 * <p>Package: {@code dk.osm2.returns} — "return" is a Java keyword.
 */
@AnalyzeClasses(packages = "dk.osm2.returns", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule r01NoPiiFieldsInNonRegistrationEntities =
            SharedArchitectureRules.NO_PII_FIELDS_IN_NON_REGISTRATION_ENTITIES;

    @ArchTest
    static final ArchRule r02EntitiesMustExtendAuditableEntity =
            SharedArchitectureRules.ENTITIES_MUST_EXTEND_AUDITABLE_ENTITY;

    @ArchTest
    static final ArchRule r03NoCrossServiceImports =
            SharedArchitectureRules.noCrossServiceImports(ServicePackages.RETURNS);

    @ArchTest
    static final ArchRule r04NoMessageBrokerImports =
            SharedArchitectureRules.NO_MESSAGE_BROKER_IMPORTS;

    @ArchTest
    static final Architectures.LayeredArchitecture r05LayeredArchitecture =
            SharedArchitectureRules.layeredArchitectureFor(ServicePackages.RETURNS);

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
}
