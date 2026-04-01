package dk.ufst.bookkeeping.model;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a period during which a specific principal balance was in effect. Used for
 * period-based interest calculation when retroactive corrections change the principal timeline.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterestPeriod {

  private LocalDate periodStart;
  private LocalDate periodEnd;
  private BigDecimal principalBalance;
  private BigDecimal annualRate;
  private long days;
  private BigDecimal interestAmount;
}
