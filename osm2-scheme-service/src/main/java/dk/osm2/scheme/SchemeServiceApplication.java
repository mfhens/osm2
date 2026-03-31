package dk.osm2.scheme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * OSM2 Scheme Service — entry point.
 *
 * <p>Stateless scheme eligibility classification for the three OSS special arrangements
 * (Non-EU, EU, Import) defined in ML §§ 66–66u and MSD arts. 358–369l.
 *
 * <p>Petition: OSS-01 — Juridisk grundlag, skemavurdering og definitioner
 */
@SpringBootApplication
public class SchemeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchemeServiceApplication.class, args);
    }
}
