package dk.osm2.taxableperson.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Default (non-demo) security configuration.
 *
 * <p>Redirects all requests to Keycloak OIDC login except actuator health endpoints.
 * The OAuth2 login stores the token in the session; downstream clients retrieve it via
 * {@link org.springframework.security.oauth2.client.OAuth2AuthorizedClientService}.
 *
 * <p>Not active when {@code spring.profiles.active=demo} — see {@link DemoSecurityConfig}.
 */
@Configuration
@EnableWebSecurity
@Profile("!demo")
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/actuator/health", "/actuator/info", "/webjars/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2Login(oauth2 -> oauth2.defaultSuccessUrl("/dashboard", true))
        .logout(
            logout ->
                logout
                    .logoutSuccessUrl("/")
                    .invalidateHttpSession(true)
                    .clearAuthentication(true));
    return http.build();
  }
}
