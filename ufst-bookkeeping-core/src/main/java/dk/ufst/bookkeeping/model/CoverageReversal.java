package dk.ufst.bookkeeping.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a dækningsophævelse: the reversal of a previously applied coverage allocation due to a
 * crossing transaction. Tracks the original allocation, the reason for reversal, and the
 * replacement allocation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoverageReversal {

  private UUID debtId;
  private UUID originalTransactionId;
  private UUID crossingEventId;
  private LocalDate effectiveDate;
  private CoverageAllocation originalAllocation;
  private CoverageAllocation replacementAllocation;
  private BigDecimal interestDelta;
  private BigDecimal principalDelta;
  private String reason;
}
