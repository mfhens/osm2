package dk.osm2.common.arch;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "dk.osm2.common", importOptions = ImportOption.DoNotIncludeTests.class)
class CommonArchitectureTest {

    @ArchTest
    static final ArchRule noSpringBootApplicationInCommon =
            SharedArchitectureRules.NO_SPRING_BOOT_APPLICATION_IN_COMMON;

    @ArchTest
    static final ArchRule noMessageBrokerImports =
            SharedArchitectureRules.NO_MESSAGE_BROKER_IMPORTS;
}
