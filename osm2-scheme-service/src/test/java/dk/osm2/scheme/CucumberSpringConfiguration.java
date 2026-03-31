package dk.osm2.scheme;

import io.cucumber.spring.CucumberContextConfiguration;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;

/**
 * Spring Boot context configuration for Cucumber integration tests.
 *
 * Starts the full application on a random port backed by an embedded PostgreSQL
 * instance (io.zonky.test:embedded-postgres) so that step definitions can hit
 * the real HTTP layer without requiring Docker.
 *
 * Petition: OSS-01 — Juridisk grundlag, skemavurdering og definitioner
 * Legal basis: ML §§ 66, 66a, 66d, 66m / MSD artikler 358, 358a, 369a, 369l
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.flyway.default-schema", () -> "scheme");
    }
}

