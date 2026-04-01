package dk.osm2.common.arch;

import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.Architectures;
import dk.ufst.opendebt.common.audit.AuditableEntity;
import jakarta.persistence.Entity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Shared ArchUnit rule definitions for the osm2 architecture.
 *
 * <p>Every rule is a static constant or factory method. Service modules import these rules
 * into a thin {@code ArchitectureTest} class annotated with {@code @AnalyzeClasses}.
 *
 * <p>Rule IDs map to the solution-architect recommendation (2026-04-01):
 * <pre>
 *   R-01  PII field isolation                [ADR-0014]
 *   R-02  AuditableEntity inheritance        [ADR-0013]
 *   R-03  No cross-service package imports   [ADR-0002, ADR-0007]
 *   R-04  No message broker imports          [ADR-0019]
 *   R-05  Layered architecture direction     [ADR-0002]
 *   R-06  Resilience4j on client methods     [ADR-0026]
 *   R-07  API URL prefix convention          [ADR-0004]
 *   R-08a Domain must not depend on Spring Web  [ADR-0001]
 *   R-08b Domain must not carry Spring stereotypes [ADR-0001]
 *   R-09  No @Entity returned from controller [ADR-0014, ADR-0002]
 *   R-10  Exceptions in .exception packages  [ADR-0002]
 * </pre>
 */
public final class SharedArchitectureRules {

    // PII field names that must not appear outside the registration-service [ADR-0014].
    private static final Pattern PII_FIELD_PATTERN = Pattern.compile(
            "(?i)(cvr|cvrnumber|legalname|postaladdress|contactdetails|representativeidentity"
            + "|representativename|registrantname|email|phonenumber|phone|bankdetails)"
    );

    // -------------------------------------------------------------------------
    // R-01 — PII Field Isolation [ADR-0014]
    //
    // No @Entity class outside dk.osm2.registration may declare PII-named fields.
    // The resideOutsideOfPackage predicate ensures registration entities are never
    // checked — they ARE the PII silo. Safe to include in ALL service tests.
    // -------------------------------------------------------------------------

    public static final ArchRule NO_PII_FIELDS_IN_NON_REGISTRATION_ENTITIES =
            classes()
                    .that().areAnnotatedWith(Entity.class)
                    .and().resideOutsideOfPackage(ServicePackages.REGISTRATION + "..")
                    .should(notHavePiiFields())
                    .allowEmptyShould(true)
                    .as("[R-01] No @Entity outside registration-service may declare PII fields [ADR-0014]");

    private static ArchCondition<JavaClass> notHavePiiFields() {
        return new ArchCondition<>("not declare PII-named fields") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                for (JavaField field : javaClass.getAllFields()) {
                    if (PII_FIELD_PATTERN.matcher(field.getName()).matches()) {
                        events.add(SimpleConditionEvent.violated(javaClass,
                                "PII field '" + field.getName() + "' found in @Entity "
                                + javaClass.getName() + " outside registration-service"));
                    }
                }
            }
        };
    }

    // -------------------------------------------------------------------------
    // R-02 — AuditableEntity Inheritance [ADR-0013]
    // -------------------------------------------------------------------------

    public static final ArchRule ENTITIES_MUST_EXTEND_AUDITABLE_ENTITY =
            classes()
                    .that().areAnnotatedWith(Entity.class)
                    .should().beAssignableTo(AuditableEntity.class)
                    .allowEmptyShould(true)
                    .as("[R-02] Every @Entity must extend AuditableEntity from audit-trail-commons [ADR-0013]");

    // -------------------------------------------------------------------------
    // R-03 — No Cross-Service Package Imports [ADR-0002, ADR-0007]
    // -------------------------------------------------------------------------

    public static ArchRule noCrossServiceImports(String ownServicePackage) {
        String[] forbidden = Arrays.stream(ServicePackages.ALL_SERVICE_ROOTS)
                .filter(p -> !p.equals(ownServicePackage))
                .map(p -> p + "..")
                .toArray(String[]::new);
        return noClasses()
                .that().resideInAPackage(ownServicePackage + "..")
                .should().dependOnClassesThat().resideInAnyPackage(forbidden)
                .allowEmptyShould(true)
                .as("[R-03] " + ownServicePackage + " must not import from other service packages"
                        + " — use REST APIs instead [ADR-0002, ADR-0007]");
    }

    // -------------------------------------------------------------------------
    // R-04 — No Message Broker Imports [ADR-0019]
    // -------------------------------------------------------------------------

    public static final ArchRule NO_MESSAGE_BROKER_IMPORTS =
            noClasses()
                    .that().resideInAPackage("dk.osm2..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.apache.kafka..",
                            "org.springframework.kafka..",
                            "com.rabbitmq..",
                            "org.springframework.amqp..",
                            "io.nats.."
                    )
                    .allowEmptyShould(true)
                    .as("[R-04] No message broker imports — orchestration over REST only [ADR-0019]");

    // -------------------------------------------------------------------------
    // R-05 — Layered Architecture Direction [ADR-0002]
    //
    // Returns a LayeredArchitecture (implements ArchRule) parameterised by the
    // service root package. consideringOnlyDependenciesInLayers() means:
    // - empty layers (absent in portals/stubs) are silently ignored
    // - deps from unlisted packages (e.g. config) are not checked
    // -------------------------------------------------------------------------

    public static Architectures.LayeredArchitecture layeredArchitectureFor(String servicePackage) {
        return Architectures.layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .withOptionalLayers(true)
                .layer("Web").definedBy(servicePackage + ".web..")
                .layer("Service").definedBy(servicePackage + ".service..")
                .layer("Repository").definedBy(servicePackage + ".repository..")
                .layer("Domain").definedBy(servicePackage + ".domain..")
                .layer("Dto").definedBy(servicePackage + ".dto..")
                .layer("Client").definedBy(servicePackage + ".client..")
                .layer("Exception").definedBy(servicePackage + ".exception..")
                .whereLayer("Web").mayNotBeAccessedByAnyLayer()
                .whereLayer("Service").mayOnlyBeAccessedByLayers("Web")
                .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service")
                .whereLayer("Client").mayOnlyBeAccessedByLayers("Web", "Service")
                .as("[R-05] Layered architecture direction must be respected [ADR-0002]");
    }

    // -------------------------------------------------------------------------
    // R-06 — Resilience4j on HTTP Client Methods [ADR-0026]
    //
    // Every public method on a class in the `.client` package must carry at
    // least one of @CircuitBreaker, @Retry, or @TimeLimiter.
    //
    // Record types (BFF-local DTOs co-located in the client package) are excluded —
    // their accessor methods are not HTTP calls and must not carry resilience annotations.
    // -------------------------------------------------------------------------

    public static final ArchRule CLIENT_METHODS_MUST_HAVE_RESILIENCE =
            methods()
                    .that().arePublic()
                    .and().areDeclaredInClassesThat().resideInAPackage("dk.osm2..client..")
                    .and().areDeclaredInClassesThat().areNotInterfaces()
                    .and().areDeclaredInClassesThat().areNotRecords()
                    .should(haveResilience4jAnnotation())
                    .as("[R-06] All public .client methods must have @CircuitBreaker/@Retry/@TimeLimiter"
                            + " [ADR-0026]")
                    .allowEmptyShould(true);

    private static ArchCondition<JavaMethod> haveResilience4jAnnotation() {
        return new ArchCondition<>("be annotated with @CircuitBreaker, @Retry, or @TimeLimiter") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                boolean hasResilience =
                        method.isAnnotatedWith(
                                "io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker")
                        || method.isAnnotatedWith(
                                "io.github.resilience4j.retry.annotation.Retry")
                        || method.isAnnotatedWith(
                                "io.github.resilience4j.timelimiter.annotation.TimeLimiter");
                if (hasResilience) {
                    events.add(SimpleConditionEvent.satisfied(method, "has Resilience4j annotation"));
                } else {
                    events.add(SimpleConditionEvent.violated(method,
                            method.getFullName() + " lacks @CircuitBreaker/@Retry/@TimeLimiter"
                            + " [ADR-0026]"));
                }
            }
        };
    }

    // -------------------------------------------------------------------------
    // R-07 — API URL Prefix Convention [ADR-0004]
    //
    // Class-level @RequestMapping on @RestController classes must start with
    // /api/ or /internal/. @Controller classes (Thymeleaf BFFs) are excluded.
    // -------------------------------------------------------------------------

    public static final ArchRule API_URL_PREFIX_CONVENTION =
            classes()
                    .that().areAnnotatedWith(
                            "org.springframework.web.bind.annotation.RestController")
                    .should(haveApiOrInternalMappingPrefix())
                    .as("[R-07] @RestController class-level @RequestMapping must start"
                            + " with /api/ or /internal/ [ADR-0004]")
                    .allowEmptyShould(true);

    private static ArchCondition<JavaClass> haveApiOrInternalMappingPrefix() {
        return new ArchCondition<>(
                "have class-level @RequestMapping with /api/ or /internal/ path prefix") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                Optional<JavaAnnotation<JavaClass>> annotOpt = javaClass.getAnnotations()
                        .stream()
                        .filter(a -> a.getRawType().getName().equals(
                                "org.springframework.web.bind.annotation.RequestMapping"))
                        .findFirst();

                if (annotOpt.isEmpty()) {
                    events.add(SimpleConditionEvent.satisfied(
                            javaClass, "no class-level @RequestMapping"));
                    return;
                }

                Object raw = annotOpt.get().get("value").orElse(null);
                String[] paths;
                if (raw instanceof String[]) {
                    paths = (String[]) raw;
                } else if (raw instanceof String) {
                    paths = new String[]{(String) raw};
                } else {
                    paths = new String[0];
                }

                if (paths.length == 0) {
                    events.add(SimpleConditionEvent.satisfied(
                            javaClass, "no path in @RequestMapping"));
                    return;
                }

                for (String path : paths) {
                    if (!path.startsWith("/api/") && !path.startsWith("/internal/")) {
                        events.add(SimpleConditionEvent.violated(javaClass,
                                "@RestController " + javaClass.getSimpleName()
                                + " has @RequestMapping(\"" + path
                                + "\") which must start with /api/ or /internal/"));
                        return;
                    }
                }
                events.add(SimpleConditionEvent.satisfied(javaClass, "path prefix OK"));
            }
        };
    }

    // -------------------------------------------------------------------------
    // R-08a — Domain Classes Must Not Depend on Spring Web [ADR-0001]
    // -------------------------------------------------------------------------

    public static final ArchRule DOMAIN_CLASSES_MUST_NOT_DEPEND_ON_SPRING_WEB =
            noClasses()
                    .that().resideInAPackage("dk.osm2..domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("org.springframework.web..")
                    .allowEmptyShould(true)
                    .as("[R-08a] Domain classes must not depend on Spring Web [ADR-0001]");

    // -------------------------------------------------------------------------
    // R-08b — Domain Classes Must Not Carry Spring Stereotypes [ADR-0001]
    // -------------------------------------------------------------------------

    public static final ArchRule DOMAIN_CLASSES_MUST_NOT_HAVE_SPRING_STEREOTYPES =
            classes()
                    .that().resideInAPackage("dk.osm2..domain..")
                    .should(notHaveSpringStereotypes())
                    .allowEmptyShould(true)
                    .as("[R-08b] Domain classes must not carry Spring stereotype annotations"
                            + " [ADR-0001]");

    private static ArchCondition<JavaClass> notHaveSpringStereotypes() {
        return new ArchCondition<>(
                "not have @Service, @Component, @Controller, or @RestController") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                boolean hasStereotype =
                        javaClass.isAnnotatedWith(Service.class)
                        || javaClass.isAnnotatedWith(Component.class)
                        || javaClass.isAnnotatedWith(Controller.class)
                        || javaClass.isAnnotatedWith(
                                "org.springframework.web.bind.annotation.RestController");
                if (hasStereotype) {
                    events.add(SimpleConditionEvent.violated(javaClass,
                            "Domain class " + javaClass.getName()
                            + " carries a Spring stereotype annotation [ADR-0001]"));
                } else {
                    events.add(SimpleConditionEvent.satisfied(
                            javaClass, "no Spring stereotypes"));
                }
            }
        };
    }

    // -------------------------------------------------------------------------
    // R-09 — No @Entity Returned Directly from @RestController [ADR-0014, ADR-0002]
    // -------------------------------------------------------------------------

    public static final ArchRule NO_ENTITY_RETURNED_FROM_CONTROLLER =
            methods()
                    .that().areDeclaredInClassesThat().areAnnotatedWith(
                            "org.springframework.web.bind.annotation.RestController")
                    .and().arePublic()
                    .should(notReturnEntityType())
                    .allowEmptyShould(true)
                    .as("[R-09] @RestController methods must not return @Entity types directly"
                            + " [ADR-0014, ADR-0002]");

    private static ArchCondition<JavaMethod> notReturnEntityType() {
        return new ArchCondition<>("not have @Entity as raw return type") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                if (method.getRawReturnType().isAnnotatedWith(Entity.class)) {
                    events.add(SimpleConditionEvent.violated(method,
                            method.getFullName() + " directly returns @Entity type "
                            + method.getRawReturnType().getName()
                            + " — use a DTO instead [ADR-0014]"));
                } else {
                    events.add(SimpleConditionEvent.satisfied(
                            method, "return type is not @Entity"));
                }
            }
        };
    }

    // -------------------------------------------------------------------------
    // R-10 — Exception Classes in .exception Package [ADR-0002]
    // -------------------------------------------------------------------------

    public static final ArchRule EXCEPTIONS_IN_EXCEPTION_PACKAGE =
            classes()
                    .that().resideInAPackage("dk.osm2..")
                    .and().areAssignableTo(RuntimeException.class)
                    .should().resideInAPackage("dk.osm2..exception..")
                    .allowEmptyShould(true)
                    .as("[R-10] RuntimeException subclasses must reside in .exception packages"
                            + " [ADR-0002]");

    // -------------------------------------------------------------------------
    // Bonus — No @SpringBootApplication in osm2-common [ADR-0002]
    // -------------------------------------------------------------------------

    public static final ArchRule NO_SPRING_BOOT_APPLICATION_IN_COMMON =
            noClasses()
                    .that().resideInAPackage(ServicePackages.COMMON + "..")
                    .should().beAnnotatedWith(
                            "org.springframework.boot.autoconfigure.SpringBootApplication")
                    .as("[Bonus] osm2-common is a shared library — @SpringBootApplication is"
                            + " forbidden [ADR-0002]");

    private SharedArchitectureRules() {}
}
