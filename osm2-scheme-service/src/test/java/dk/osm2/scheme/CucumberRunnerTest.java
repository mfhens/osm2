package dk.osm2.scheme;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * Cucumber JUnit Platform Suite runner for OSS-01 BDD tests.
 *
 * Petition: OSS-01 — Juridisk grundlag, skemavurdering og definitioner
 * Legal basis: ML §§ 66, 66a, 66d, 66m / MSD artikler 358, 358a, 369a, 369l
 *
 * These tests are EXPECTED TO FAIL until the production code in
 * dk.osm2.scheme is implemented (DTOs, controller, service, rules engine).
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "dk.osm2.scheme.steps,dk.osm2.scheme")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty,html:target/cucumber-reports/report.html")
public class CucumberRunnerTest {
}
