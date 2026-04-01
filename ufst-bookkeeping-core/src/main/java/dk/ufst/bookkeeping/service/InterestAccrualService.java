package dk.ufst.bookkeeping.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import dk.ufst.bookkeeping.model.InterestPeriod;

/**
 * Period-based interest calculation service. Calculates interest across multiple periods where the
 * principal balance may change (due to payments, corrections, udlaeg adjustments, etc.).
 */
public interface InterestAccrualService {

  /**
   * Calculates interest for each period in the debt's timeline from the given start date.
   *
   * @param debtId the debt to calculate interest for
   * @param fromDate the start date for calculation
   * @param toDate the end date for calculation
   * @param annualRate the annual interest rate as a decimal (e.g., 0.10 for 10%)
   * @return ordered list of interest periods with calculated amounts
   */
  List<InterestPeriod> calculatePeriodicInterest(
      UUID debtId, LocalDate fromDate, LocalDate toDate, BigDecimal annualRate);
}
