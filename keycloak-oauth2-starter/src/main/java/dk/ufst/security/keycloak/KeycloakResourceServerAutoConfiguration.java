package dk.ufst.security.keycloak;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Boot auto-configuration for Keycloak OAuth2 resource server.
 *
 * <p>Provides two profile-gated {@link SecurityFilterChain} beans:
 *
 * <ul>
 *   <li><b>keycloakSecuredFilterChain</b> — active for {@code !local & !dev & !demo}: stateless JWT
 *       resource server, CSRF disabled, actuator + Swagger UI permitted.
 *   <li><b>keycloakPermissiveFilterChain</b> — active for {@code local | dev | demo}: permits all
 *       requests without authentication for local development.
 * </ul>
 *
 * <p>Both beans are guarded by {@link ConditionalOnMissingBean} so any service can declare its own
 * bean with the same name to override the starter's default.
 */
@AutoConfiguration
@EnableWebSecurity
@EnableConfigurationProperties(KeycloakStarterProperties.class)
public class KeycloakResourceServerAutoConfiguration {

  @Bean("keycloakSecuredFilterChain")
  @Profile("!local & !dev & !demo")
  @ConditionalOnMissingBean(name = "keycloakSecuredFilterChain")
  @SuppressWarnings("java:S4502")
  public SecurityFilterChain keycloakSecuredFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/**")
                    .permitAll()
                    .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));
    return http.build();
  }

  @Bean("keycloakPermissiveFilterChain")
  @Profile("local | dev | demo")
  @ConditionalOnMissingBean(name = "keycloakPermissiveFilterChain")
  @SuppressWarnings("java:S4502")
  public SecurityFilterChain keycloakPermissiveFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    return http.build();
  }
}
