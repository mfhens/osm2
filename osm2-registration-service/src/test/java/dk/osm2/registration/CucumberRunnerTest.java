package dk.osm2.registration;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * Cucumber JUnit Platform Suite runner for OSS-02 BDD tests.
 *
 * Petition: OSS-02 — Registrering og afmeldelse: Ikke-EU-ordning og EU-ordning
 * Legal basis: ML §§ 66a–66j; Momsbekendtgørelsen §§ 115–119; Momsforordningen artikler 57d–58c
 *
 * These tests are EXPECTED TO FAIL until the production code in
 * dk.osm2.registration is implemented (DTOs, controllers, services, domain model).
 *
 * FAILURE MODE: Compile-time — all references to dk.osm2.registration.dto.*
 * records prevent compilation until those types are created.
 * Once DTOs exist, tests will fail at runtime because REST endpoints
 * return 404 / are not yet implemented.
 *
 * Coverage: 49 Gherkin scenarios across 8 Feature blocks.
 * FR refs: FR-OSS-02-001 through FR-OSS-02-038.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "dk.osm2.registration.steps,dk.osm2.registration")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty,html:target/cucumber-reports/oss02-report.html")
public class CucumberRunnerTest {
}
