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
 * Represents how a single dækning (recovery/payment) is allocated between interest and principal,
 * following dækningsrækkefølge: inddrivelsesrente is always covered before hovedstol.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoverageAllocation {

  private UUID debtId;
  private UUID sourceEventId;
  private LocalDate effectiveDate;
  private BigDecimal totalAmount;
  private BigDecimal interestPortion;
  private BigDecimal feesPortion;
  private BigDecimal principalPortion;
  private BigDecimal accruedInterestAtDate;
  private BigDecimal outstandingFeesAtDate;
  private BigDecimal principalBalanceAtDate;
}
