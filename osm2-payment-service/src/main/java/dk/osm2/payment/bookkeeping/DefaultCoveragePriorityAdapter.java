package dk.osm2.payment.bookkeeping;

import dk.ufst.bookkeeping.model.CoverageAllocation;
import dk.ufst.bookkeeping.port.CoveragePriorityPort;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Default dækningsrækkefølge (coverage priority order) for OSM2 VAT OSS debt collection.
 *
 * <p>Payment is applied in the following order per Inddrivelsesloven § 4, stk. 1:
 *
 * <ol>
 *   <li>Inddrivelsesrente (accrued interest) — covered first
 *   <li>Fees (outstandingFees)
 *   <li>Principal (OSS VAT debt)
 * </ol>
 */
public class DefaultCoveragePriorityAdapter implements CoveragePriorityPort {

  @Override
  public CoverageAllocation allocatePayment(
      UUID debtId,
      BigDecimal paymentAmount,
      BigDecimal accruedInterest,
      BigDecimal outstandingFees,
      BigDecimal principalBalance) {

    BigDecimal remaining = paymentAmount;

    BigDecimal interestPortion = remaining.min(accruedInterest);
    remaining = remaining.subtract(interestPortion);

    BigDecimal feesPortion = remaining.min(outstandingFees);
    remaining = remaining.subtract(feesPortion);

    BigDecimal principalPortion = remaining.min(principalBalance);

    return CoverageAllocation.builder()
        .debtId(debtId)
        .totalAmount(paymentAmount)
        .interestPortion(interestPortion)
        .feesPortion(feesPortion)
        .principalPortion(principalPortion)
        .accruedInterestAtDate(accruedInterest)
        .outstandingFeesAtDate(outstandingFees)
        .principalBalanceAtDate(principalBalance)
        .build();
  }
}
