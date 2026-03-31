package dk.osm2.taxableperson;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Taxable Person Portal BFF.
 *
 * <p>Serves Thymeleaf views; all backend calls are delegated to downstream microservices via OkHttp.
 */
@SpringBootApplication
public class TaxablePersonPortalApplication {

  public static void main(String[] args) {
    SpringApplication.run(TaxablePersonPortalApplication.class, args);
  }
}
