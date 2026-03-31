package dk.ufst.opendebt.common.audit.cls;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

/**
 * Audit event DTO for Common Logging System (CLS) integration.
 *
 * <p>This DTO represents an audit event in the format expected by UFST's Common Logging System.
 * Events are shipped asynchronously for compliance and security monitoring.
 *
 * <p>Field mappings from OpenDebt audit_log table:
 *
 * <ul>
 *   <li>eventId - Generated UUID for the event
 *   <li>timestamp - audit_log.timestamp
 *   <li>serviceName - Application/service name
 *   <li>operation - audit_log.operation (INSERT/UPDATE/DELETE)
 *   <li>resourceType - audit_log.table_name
 *   <li>resourceId - audit_log.record_id
 *   <li>userId - audit_log.application_user
 *   <li>clientIp - audit_log.client_ip
 *   <li>changedFields - audit_log.changed_fields
 *   <li>oldValues - audit_log.old_values (masked for PII)
 *   <li>newValues - audit_log.new_values (masked for PII)
 * </ul>
 */
@Data
@Builder
public class ClsAuditEvent {

  /** Unique identifier for this audit event */
  private UUID eventId;

  /** When the event occurred (ISO-8601) */
  private Instant timestamp;

  /** OpenDebt service name (e.g., creditor-service, debt-service) */
  private String serviceName;

  /** Type of operation: INSERT, UPDATE, DELETE */
  private String operation;

  /** Database table/resource type */
  private String resourceType;

  /** Primary key of the affected record */
  private UUID resourceId;

  /** User who performed the action (from JWT/authentication) */
  private String userId;

  /** Client IP address */
  private String clientIp;

  /** Client application identifier */
  private String clientApplication;

  /** Database transaction ID for correlation */
  private Long transactionId;

  /** List of fields that were changed (for UPDATE operations) */
  private List<String> changedFields;

  /** Previous values (PII fields masked) */
  private Map<String, Object> oldValues;

  /** New values (PII fields masked) */
  private Map<String, Object> newValues;

  /** Correlation ID for request tracing */
  private String correlationId;

  /** Environment identifier (dev, test, prod) */
  private String environment;
}
