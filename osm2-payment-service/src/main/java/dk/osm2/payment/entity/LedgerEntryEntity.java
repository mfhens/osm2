package dk.osm2.payment.entity;

import dk.ufst.bookkeeping.domain.EntryCategory;
import dk.ufst.bookkeeping.domain.EntryType;
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
 * JPA entity for one side of a double-entry ledger posting.
 *
 * <p>Each balanced transaction produces exactly two rows (DEBIT + CREDIT) sharing the same {@code
 * transactionId}. Storno entries carry a non-null {@code reversalOfTransactionId} linking them to
 * the original transaction they cancel.
 *
 * <p>A tamper-evident copy of every entry is written to immudb on insert [ADR-0029].
 */
@Entity
@Table(name = "ledger_entry", schema = "payment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntryEntity extends AuditableEntity {

  @Id
  @GeneratedValue
  @UuidGenerator
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "transaction_id", nullable = false)
  private UUID transactionId;

  @Column(name = "debt_id", nullable = false)
  private UUID debtId;

  @Column(name = "account_code", nullable = false, length = 20)
  private String accountCode;

  @Column(name = "account_name", nullable = false, length = 100)
  private String accountName;

  @Enumerated(EnumType.STRING)
  @Column(name = "entry_type", nullable = false, length = 10)
  private EntryType entryType;

  @Column(name = "amount", nullable = false, precision = 19, scale = 4)
  private BigDecimal amount;

  @Column(name = "effective_date", nullable = false)
  private LocalDate effectiveDate;

  @Column(name = "posting_date", nullable = false)
  private LocalDate postingDate;

  @Column(name = "reference", length = 255)
  private String reference;

  @Column(name = "description", length = 500)
  private String description;

  /** Non-null for storno entries; references the transactionId of the reversed transaction. */
  @Column(name = "reversal_of_transaction_id")
  private UUID reversalOfTransactionId;

  @Enumerated(EnumType.STRING)
  @Column(name = "entry_category", nullable = false, length = 30)
  private EntryCategory entryCategory;
}
