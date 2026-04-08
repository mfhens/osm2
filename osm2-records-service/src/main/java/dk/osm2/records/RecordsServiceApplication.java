package dk.osm2.records;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * OSM2 Records Service — entry point.
 *
 * <p>Authority-facing records and audit-related APIs. Implementation is incremental; this boots
 * the service with Flyway, JPA, and actuator per {@code application.yml}.
 */
@SpringBootApplication
public class RecordsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecordsServiceApplication.class, args);
    }
}
