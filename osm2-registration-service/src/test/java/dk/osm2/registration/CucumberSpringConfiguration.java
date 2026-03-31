package dk.osm2.registration;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Spring Boot context configuration for OSS-02 Cucumber integration tests.
 *
 * Starts the full registration-service application on a random port backed by a
 * Testcontainers PostgreSQL instance so that step definitions can exercise the
 * real HTTP layer end-to-end.
 *
 * @ActiveProfiles("demo") activates the permissive security configuration
 * provided by keycloak-oauth2-starter, bypassing OAuth2 token requirements
 * so Cucumber can call the API without a real Keycloak instance.
 *
 * Petition: OSS-02
 * Legal basis: ML §§ 66a–66j
 * FR refs: FR-OSS-02-001 through FR-OSS-02-038
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("demo")
@Testcontainers
public class CucumberSpringConfiguration {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("osm2_registration")
            .withUsername("osm2_registration")
            .withPassword("osm2_registration");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.default-schema", () -> "registration");
    }
}
