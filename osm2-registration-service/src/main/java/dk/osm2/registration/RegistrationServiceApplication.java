package dk.osm2.registration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * OSM2 Registration Service — entry point.
 *
 * <p>Handles the registration and deregistration lifecycle for the three OSS special arrangements
 * (Non-EU, EU, Import) defined in ML §§ 66a–66j and Momsbekendtgørelsen §§ 115–119.
 *
 * <p>Petition: OSS-02 — Registrering og afmeldelse
 * <p>Legal basis: ML §§ 66a–66j; Momsbekendtgørelsen §§ 115–119
 */
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "registrationAuditorAware")
public class RegistrationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RegistrationServiceApplication.class, args);
    }
}
