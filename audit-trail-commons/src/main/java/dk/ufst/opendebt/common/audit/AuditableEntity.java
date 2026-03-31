package dk.ufst.opendebt.common.audit;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.SourceType;
import org.hibernate.generator.EventType;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Base class for auditable entities providing standard audit fields.
 *
 * <p>All entities that require audit tracking should extend this class. It provides:
 *
 * <ul>
 *   <li>createdAt - Timestamp of entity creation (auto-populated)
 *   <li>updatedAt - Timestamp of last modification (auto-populated)
 *   <li>createdBy - User who created the entity (from security context)
 *   <li>updatedBy - User who last modified the entity (from security context)
 *   <li>version - Optimistic locking version
 * </ul>
 *
 * <p>Works in conjunction with:
 *
 * <ul>
 *   <li>{@link AuditContextService} - Sets PostgreSQL session audit context
 *   <li>{@link AuditContextFilter} - Captures user/IP from HTTP requests
 *   <li>Database audit triggers - Capture all changes to audit_log table
 * </ul>
 *
 * <p>Usage:
 *
 * <pre>{@code
 * @Entity
 * public class MyEntity extends AuditableEntity {
 *     @Id
 *     private UUID id;
 *     // ... other fields
 * }
 * }</pre>
 *
 * @see AuditContextService
 * @see AuditContextFilter
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class AuditableEntity {

  @CurrentTimestamp(event = EventType.INSERT, source = SourceType.VM)
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @CurrentTimestamp(source = SourceType.VM)
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @CreatedBy
  @Column(name = "created_by", length = 100)
  private String createdBy;

  @LastModifiedBy
  @Column(name = "updated_by", length = 100)
  private String updatedBy;

  @Version
  @Column(name = "version")
  private Long version;
}
