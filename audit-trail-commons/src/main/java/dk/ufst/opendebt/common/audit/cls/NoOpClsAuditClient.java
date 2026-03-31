package dk.ufst.opendebt.common.audit.cls;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * No-op implementation of CLS audit client for development/testing.
 *
 * <p>Used when CLS integration is disabled (opendebt.audit.cls.enabled=false or not set). Events
 * are logged at DEBUG level but not shipped.
 */
@Slf4j
@Component
@ConditionalOnProperty(
    name = "opendebt.audit.cls.enabled",
    havingValue = "false",
    matchIfMissing = true)
public class NoOpClsAuditClient implements ClsAuditClient {

  @Override
  public void shipEvent(ClsAuditEvent event) {
    log.debug("CLS disabled, audit event not shipped: {}", event.getEventId());
  }

  @Override
  public void shipEvents(List<ClsAuditEvent> events) {
    log.debug("CLS disabled, {} audit events not shipped", events.size());
  }

  @Override
  public void flush() {
    // No-op
  }

  @Override
  public boolean isEnabled() {
    return false;
  }
}
