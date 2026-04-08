package dk.osm2.taxableperson;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

/**
 * Entry point for the Taxable Person Portal BFF.
 *
 * <p>Serves Thymeleaf views; all backend calls are delegated to downstream microservices via OkHttp.
 *
 * <p>No local persistence — osm2-common brings JPA transitively; JDBC/Hibernate auto-config is
 * excluded so the BFF starts without a {@code spring.datasource} URL.
 */
@SpringBootApplication(
    exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class TaxablePersonPortalApplication {

  public static void main(String[] args) {
    SpringApplication.run(TaxablePersonPortalApplication.class, args);
  }
}
