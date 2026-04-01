package dk.ufst.bookkeeping.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import dk.ufst.bookkeeping.model.TimelineReplayResult;

/**
 * Replays the full financial timeline for a debt from a crossing point, recalculating interest,
 * re-applying dækningsrækkefølge, generating dækningsophævelser, and posting corrected ledger
 * entries.
 */
public interface TimelineReplayService {

  TimelineReplayResult replayTimeline(
      UUID debtId,
      LocalDate crossingPoint,
      BigDecimal annualInterestRate,
      String triggeringReference);
}
