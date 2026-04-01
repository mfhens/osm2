package dk.ufst.bookkeeping.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Immutable domain event in the debt financial timeline. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialEvent {

  /** Null for unsaved events; set after persistence. */
  private UUID id;

  private UUID debtId;
  private EventType eventType;
  private LocalDate effectiveDate;
  private BigDecimal amount;
  private UUID correctsEventId;
  private String reference;
  private String description;
  private UUID ledgerTransactionId;

  /** Null for unsaved events; set after persistence. */
  private LocalDateTime createdAt;
}
