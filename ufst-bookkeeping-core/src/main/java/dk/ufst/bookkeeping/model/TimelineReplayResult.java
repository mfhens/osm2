package dk.ufst.bookkeeping.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Result of a full timeline replay triggered by a crossing transaction. Contains all storno
 * entries, recalculated interest periods, coverage reallocations, and coverage reversals.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimelineReplayResult {

  private UUID debtId;
  private UUID triggeringEventId;
  private LocalDate replayFromDate;
  private LocalDate replayToDate;
  private int stornoEntriesPosted;
  private int newEntriesPosted;
  private List<InterestPeriod> recalculatedInterestPeriods;
  private List<CoverageAllocation> recalculatedAllocations;
  private List<CoverageReversal> coverageReversals;
  private BigDecimal oldInterestTotal;
  private BigDecimal newInterestTotal;
  private BigDecimal interestDelta;
  private BigDecimal finalPrincipalBalance;
  private BigDecimal finalInterestBalance;
}
