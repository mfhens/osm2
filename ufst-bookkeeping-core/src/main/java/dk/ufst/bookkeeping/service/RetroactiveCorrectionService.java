package dk.ufst.bookkeeping.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import dk.ufst.bookkeeping.model.CorrectionResult;

/**
 * Service for handling retroactive corrections that require storno of existing ledger entries and
 * recalculation of downstream effects (interest).
 */
public interface RetroactiveCorrectionService {

  CorrectionResult applyRetroactiveCorrection(
      UUID debtId,
      LocalDate effectiveDate,
      BigDecimal originalAmount,
      BigDecimal correctedAmount,
      BigDecimal annualInterestRate,
      String reference,
      String reason);
}
