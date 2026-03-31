package dk.ufst.opendebt.common.audit.cls;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Maps database audit records to CLS audit events.
 *
 * <p>Handles PII masking to ensure sensitive data is not transmitted to CLS. PII fields are
 * replaced with "[MASKED]" placeholder.
 */
@Component
public class ClsAuditEventMapper {

  private static final String MASKED_VALUE = "[MASKED]";

  /** Fields that contain PII and must be masked before CLS transmission */
  private static final Set<String> PII_FIELDS =
      Set.of(
          "cpr_number",
          "cpr",
          "cvr_number",
          "cvr",
          "name",
          "first_name",
          "last_name",
          "full_name",
          "address",
          "street",
          "city",
          "postal_code",
          "zip_code",
          "email",
          "phone",
          "mobile",
          "telephone",
          "encrypted_cpr",
          "encrypted_cvr",
          "encrypted_name",
          "encrypted_address",
          "bank_account",
          "iban",
          "account_number");

  @Value("${spring.application.name:opendebt}")
  private String serviceName;

  @Value("${spring.profiles.active:dev}")
  private String environment;

  /**
   * Creates a CLS audit event from database audit record fields.
   *
   * @param tableName The database table that was modified
   * @param recordId The primary key of the modified record
   * @param operation The operation type (INSERT, UPDATE, DELETE)
   * @param oldValues Previous field values (may be null for INSERT)
   * @param newValues New field values (may be null for DELETE)
   * @param changedFields List of fields that changed (for UPDATE)
   * @param userId The user who made the change
   * @param clientIp The client's IP address
   * @param clientApp The client application identifier
   * @param transactionId Database transaction ID
   * @param timestamp When the change occurred
   * @return A CLS audit event ready for shipping
   */
  public ClsAuditEvent mapToClsEvent(
      String tableName,
      UUID recordId,
      String operation,
      Map<String, Object> oldValues,
      Map<String, Object> newValues,
      String[] changedFields,
      String userId,
      String clientIp,
      String clientApp,
      Long transactionId,
      Instant timestamp) {

    return ClsAuditEvent.builder()
        .eventId(UUID.randomUUID())
        .timestamp(timestamp)
        .serviceName(serviceName)
        .operation(operation)
        .resourceType(tableName)
        .resourceId(recordId)
        .userId(userId)
        .clientIp(clientIp)
        .clientApplication(clientApp)
        .transactionId(transactionId)
        .changedFields(changedFields != null ? Arrays.asList(changedFields) : List.of())
        .oldValues(maskPiiFields(oldValues))
        .newValues(maskPiiFields(newValues))
        .environment(environment)
        .build();
  }

  /**
   * Masks PII fields in a value map.
   *
   * @param values The original field values
   * @return A new map with PII fields masked
   */
  private Map<String, Object> maskPiiFields(Map<String, Object> values) {
    if (values == null) {
      return null;
    }

    Map<String, Object> masked = new HashMap<>();
    for (Map.Entry<String, Object> entry : values.entrySet()) {
      String key = entry.getKey().toLowerCase();
      if (PII_FIELDS.contains(key) || isPiiField(key)) {
        masked.put(entry.getKey(), MASKED_VALUE);
      } else {
        masked.put(entry.getKey(), entry.getValue());
      }
    }
    return masked;
  }

  /** Additional heuristic check for PII field names. */
  private boolean isPiiField(String fieldName) {
    String lower = fieldName.toLowerCase();
    return lower.contains("person") && lower.contains("name")
        || lower.contains("ssn")
        || lower.contains("social_security")
        || lower.contains("passport")
        || lower.contains("driver_license");
  }
}
