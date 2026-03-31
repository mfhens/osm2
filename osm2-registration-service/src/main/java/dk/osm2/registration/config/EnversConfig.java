package dk.osm2.registration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Hibernate Envers configuration for bitemporal audit tracking.
 *
 * <p>Envers tracks transaction time (registreringstid) for all {@code @Audited} entities,
 * fulfilling ML §§ 66b, 66e, 66n and momsforordningen art. 58 stk. 2.
 *
 * <p>Petition: OSS-02
 */
@Configuration
@EnableJpaRepositories(
        basePackages = "dk.osm2.registration.repository",
        repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean.class)
public class EnversConfig {

    /**
     * AuditorAware bean for {@code @CreatedBy} / {@code @LastModifiedBy} fields in AuditableEntity.
     *
     * <p>In production, returns the authenticated user from the security context.
     * In the demo profile, returns "system" as the auditor.
     */
    @Bean("registrationAuditorAware")
    public AuditorAware<String> registrationAuditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                return Optional.of(auth.getName());
            }
            return Optional.of("system");
        };
    }
}
