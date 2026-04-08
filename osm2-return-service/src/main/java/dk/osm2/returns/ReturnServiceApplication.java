package dk.osm2.returns;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * OSM2 Return Service — entry point.
 *
 * <p>VAT return filing, corrections, and late-return reminders (OSS-04). Implementation is
 * incremental; this class boots the service with Flyway, JPA, and actuator as configured in
 * {@code application.yml}.
 */
@SpringBootApplication
public class ReturnServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReturnServiceApplication.class, args);
    }
}
