package dk.osm2.taxableperson.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the <strong>demo</strong> Spring profile.
 *
 * <p>In demo mode all requests are permitted without authentication so that the portal can be
 * explored locally without a running Keycloak instance. CSRF is disabled for convenience.
 *
 * <p>This bean replaces {@link SecurityConfig} when {@code spring.profiles.active=demo}.
 */
@Configuration
@EnableWebSecurity
@Profile("demo")
public class DemoSecurityConfig {

  @Bean
  public SecurityFilterChain demoSecurityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .csrf(csrf -> csrf.disable());
    return http.build();
  }
}
