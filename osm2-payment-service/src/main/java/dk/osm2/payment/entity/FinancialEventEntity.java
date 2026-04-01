package dk.osm2.payment.entity;

import dk.ufst.bookkeeping.domain.EventType;
import dk.ufst.opendebt.common.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

/**
 * JPA entity for a domain event in the debt financial timeline.
 *
 * <p>One row per occurrence (debt registration, payment received, interest accrual, etc.). The
 * ordered timeline of events is replayed by {@code TimelineReplayService} and {@code
 * InterestAccrualService} to compute running balances and recalculate interest.
 *
 * <p>{@code createdAt} (from {@code AuditableEntity}) is propagated back to the {@code
 * FinancialEvent} domain object after persistence so callers see the authoritative timestamp.
 */
@Entity
@Table(name = "financial_event", schema = "payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialEventEntity extends AuditableEntity {

  @Id
  @GeneratedValue
  @UuidGenerator
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "debt_id", nullable = false)
  private UUID debtId;

  @Enumerated(EnumType.STRING)
  @Column(name = "event_type", nullable = false, length = 30)
  private EventType eventType;

  @Column(name = "effective_date", nullable = false)
  private LocalDate effectiveDate;

  @Column(name = "amount", nullable = false, precision = 19, scale = 4)
  private BigDecimal amount;

  /** Non-null for CORRECTION events; references the original event being corrected. */
  @Column(name = "corrects_event_id")
  private UUID correctsEventId;

  @Column(name = "reference", length = 255)
  private String reference;

  @Column(name = "description", length = 500)
  private String description;

  /** Links this event to its corresponding double-entry ledger transaction. */
  @Column(name = "ledger_transaction_id")
  private UUID ledgerTransactionId;
}
