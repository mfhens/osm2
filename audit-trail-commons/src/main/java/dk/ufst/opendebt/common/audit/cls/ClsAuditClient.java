package dk.ufst.opendebt.common.audit.cls;

import java.util.List;

/**
 * Client interface for shipping audit events to UFST Common Logging System (CLS).
 *
 * <p>Implementations should handle:
 *
 * <ul>
 *   <li>Asynchronous event shipping to minimize latency impact
 *   <li>Retry logic for transient failures
 *   <li>Batching for efficiency
 *   <li>PII masking before transmission
 *   <li>mTLS authentication with CLS endpoints
 * </ul>
 *
 * <p>Configuration properties:
 *
 * <pre>
 * opendebt.audit.cls:
 *   enabled: true
 *   endpoint: https://cls.ufst.dk/api/v1/events
 *   batch-size: 100
 *   flush-interval-ms: 5000
 *   retry-attempts: 3
 * </pre>
 *
 * @see ClsAuditEvent
 * @see ClsAuditClientImpl
 */
public interface ClsAuditClient {

  /**
   * Ships a single audit event to CLS.
   *
   * @param event The audit event to ship
   */
  void shipEvent(ClsAuditEvent event);

  /**
   * Ships multiple audit events to CLS in a batch.
   *
   * @param events The audit events to ship
   */
  void shipEvents(List<ClsAuditEvent> events);

  /** Flushes any pending events immediately. Called during application shutdown. */
  void flush();

  /**
   * Checks if the CLS client is enabled and connected.
   *
   * @return true if events will be shipped, false if disabled/disconnected
   */
  boolean isEnabled();
}
