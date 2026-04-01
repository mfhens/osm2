package dk.ufst.bookkeeping.port;

import java.math.BigDecimal;
import java.util.UUID;

import dk.ufst.bookkeeping.model.CoverageAllocation;

public interface CoveragePriorityPort {

  CoverageAllocation allocatePayment(
      UUID debtId,
      BigDecimal paymentAmount,
      BigDecimal accruedInterest,
      BigDecimal outstandingFees,
      BigDecimal principalBalance);
}
