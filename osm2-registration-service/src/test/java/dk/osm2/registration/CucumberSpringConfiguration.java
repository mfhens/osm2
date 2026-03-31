package dk.osm2.registration;

import io.cucumber.spring.CucumberContextConfiguration;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;

/**
 * Spring Boot context configuration for OSS-02 Cucumber integration tests.
 *
 * Starts the full registration-service application on a random port backed by an
 * embedded PostgreSQL instance (io.zonky.test:embedded-postgres) so that step
 * definitions can exercise the real HTTP layer end-to-end without Docker.
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
public class CucumberSpringConfiguration {

    static final EmbeddedPostgres POSTGRES;

    static {
        try {
            POSTGRES = EmbeddedPostgres.start();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> POSTGRES.getJdbcUrl("postgres", "postgres")
                        + "&currentSchema=registration&stringtype=unspecified");
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.flyway.default-schema", () -> "registration");
        registry.add("server.error.include-message", () -> "always");
    }
}

