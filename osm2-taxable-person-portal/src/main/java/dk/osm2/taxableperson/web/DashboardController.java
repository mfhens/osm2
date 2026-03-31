package dk.osm2.taxableperson.web;

import dk.osm2.taxableperson.client.RegistrationResponse;
import dk.osm2.taxableperson.client.RegistrationServiceClient;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Dashboard controller — displays the list of VAT OSS registrations for the current user.
 *
 * <p>In demo mode ({@code @Profile("demo")}) a hardcoded UUID is used as the registrant ID.
 * In production the OIDC {@code sub} claim is extracted from the principal.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DashboardController {

  /** Demo registrant UUID — used when no OIDC token is present (demo profile). */
  private static final UUID DEMO_REGISTRANT_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  private final RegistrationServiceClient registrationServiceClient;

  @GetMapping({"/", "/dashboard"})
  public String dashboard(Authentication authentication, Model model) {
    UUID registrantId = resolveRegistrantId(authentication);
    log.debug("Loading dashboard for registrantId={}", registrantId);

    List<RegistrationResponse> registrations;
    try {
      registrations = registrationServiceClient.listMyRegistrations(registrantId);
    } catch (Exception e) {
      log.warn("Could not load registrations for {}: {}", registrantId, e.getMessage());
      registrations = Collections.emptyList();
      model.addAttribute("loadError", true);
    }

    model.addAttribute("registrations", registrations);
    model.addAttribute("registrantId", registrantId);
    return "dashboard";
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  /**
   * Resolves the registrant UUID from the current authentication.
   *
   * <ul>
   *   <li>If the principal is an {@link OidcUser} the {@code sub} claim is used.
   *   <li>Otherwise (demo mode, anonymous) the hardcoded demo UUID is used.
   * </ul>
   */
  private UUID resolveRegistrantId(Authentication authentication) {
    if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
      Object principal = oauthToken.getPrincipal();
      if (principal instanceof OidcUser oidcUser) {
        String sub = oidcUser.getSubject();
        if (sub != null) {
          try {
            return UUID.fromString(sub);
          } catch (IllegalArgumentException ex) {
            log.warn("OIDC sub '{}' is not a UUID — using name-based UUID", sub);
            return UUID.nameUUIDFromBytes(sub.getBytes());
          }
        }
      }
    }
    return DEMO_REGISTRANT_ID;
  }
}
