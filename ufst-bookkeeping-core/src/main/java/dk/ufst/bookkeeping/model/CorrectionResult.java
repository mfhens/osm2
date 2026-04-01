package dk.ufst.bookkeeping.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Result of a retroactive correction, detailing all storno and new entries posted. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorrectionResult {

  private UUID debtId;
  private UUID correctionEventId;
  private BigDecimal principalDelta;
  private int stornoEntriesPosted;
  private int newInterestEntriesPosted;
  private BigDecimal oldInterestTotal;
  private BigDecimal newInterestTotal;
  private BigDecimal interestDelta;
  private List<InterestPeriod> recalculatedPeriods;
}
