package dk.ufst.security.keycloak;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Keycloak OAuth2 resource server starter.
 *
 * <p>Bind via {@code keycloak.starter.*} in {@code application.properties} / {@code
 * application.yml}.
 */
@ConfigurationProperties(prefix = "keycloak.starter")
public class KeycloakStarterProperties {

  /** URL patterns always permitted without authentication (actuator, Swagger). */
  private List<String> permittedPaths =
      List.of("/actuator/**", "/api-docs/**", "/swagger-ui/**", "/swagger-ui.html");

  public List<String> getPermittedPaths() {
    return permittedPaths;
  }

  public void setPermittedPaths(List<String> permittedPaths) {
    this.permittedPaths = permittedPaths;
  }
}
