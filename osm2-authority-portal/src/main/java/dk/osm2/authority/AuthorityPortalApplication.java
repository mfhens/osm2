package dk.osm2.authority;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

/** Authority BFF — no local database; excludes JDBC/Hibernate auto-config from transitive JPA. */
@SpringBootApplication(
    exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class AuthorityPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthorityPortalApplication.class, args);
    }
}
