package dk.ufst.opendebt.common.audit;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Configuration for JPA auditing.
 *
 * <p>Enables automatic population of @CreatedBy and @LastModifiedBy fields based on the current
 * security context.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditingConfig {

  @Bean
  public AuditorAware<String> auditorProvider() {
    return () -> {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication == null || !authentication.isAuthenticated()) {
        return Optional.of("system");
      }
      String name = authentication.getName();
      if ("anonymousUser".equals(name)) {
        return Optional.of("anonymous");
      }
      return Optional.of(name);
    };
  }
}
