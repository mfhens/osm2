package dk.ufst.opendebt.common.audit;

import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;

/**
 * Spring Boot auto-configuration for audit trail commons.
 *
 * <p>Activates only when a {@link DataSource} bean exists (after JDBC auto-configuration). BFFs
 * that depend on osm2-common but do not configure a database therefore skip this configuration
 * entirely. All beans are guarded by {@link ConditionalOnMissingBean} so services that already
 * register these components via component-scan (e.g., opendebt-integration-gateway) are not
 * affected.
 *
 * <p>Registered via
 * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
 */
@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@ConditionalOnBean(DataSource.class)
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
