package dk.ufst.opendebt.common.audit;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;

/**
 * Spring Boot auto-configuration for audit trail commons.
 *
 * <p>Activates when a {@link DataSource} is present on the classpath (i.e., any service with JPA).
 * All beans are guarded by {@link ConditionalOnMissingBean} so services that already register
 * these components via component-scan (e.g., opendebt-integration-gateway) are not affected.
 *
 * <p>Registered via META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
 */
@AutoConfiguration
@ConditionalOnClass(DataSource.class)
public class AuditAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(AuditContextService.class)
  public AuditContextService auditContextService(DataSource dataSource) {
    return new AuditContextService(dataSource);
  }

  @Bean
  @ConditionalOnMissingBean(AuditContextFilter.class)
  public AuditContextFilter auditContextFilter(AuditContextService auditContextService) {
    return new AuditContextFilter(auditContextService);
  }

  @Bean
  @ConditionalOnMissingBean(AuditorAware.class)
  public AuditorAware<String> auditorProvider() {
    return new AuditingConfig().auditorProvider();
  }
}
