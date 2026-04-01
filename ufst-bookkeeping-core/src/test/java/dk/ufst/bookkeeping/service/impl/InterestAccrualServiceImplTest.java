package dk.ufst.bookkeeping.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.bookkeeping.domain.EventType;
import dk.ufst.bookkeeping.domain.FinancialEvent;
import dk.ufst.bookkeeping.model.InterestPeriod;
import dk.ufst.bookkeeping.port.FinancialEventStore;

/**
 * Unit tests for InterestAccrualServiceImpl.
 *
 * <p>Every test targets a specific financial correctness invariant. Concrete numbers are used
 * throughout so that failures are immediately actionable. The 365-day basis, zero-principal guard,
 * and period-boundary semantics are all explicitly exercised.
 */
@ExtendWith(MockitoExtension.class)
class InterestAccrualServiceImplTest {

  @Mock private FinancialEventStore financialEventStore;

  private InterestAccrualServiceImpl service;

  private static final UUID DEBT_ID = UUID.randomUUID();
  private static final BigDecimal ANNUAL_RATE_10_PCT = new BigDecimal("0.10");

  @BeforeEach
  void setUp() {
    service = new InterestAccrualServiceImpl(financialEventStore);
  }

  // ---------------------------------------------------------------------------
  // Zero principal / zero balance
  // ---------------------------------------------------------------------------

  @Test
  void givenZeroPrincipal_whenCalculateInterest_thenReturnsEmptyPeriods() {
    // Ref: InterestAccrualServiceImpl — balance <= 0 guard skips period creation.
    // No events → balance timeline contains (fromDate, 0) → nothing emitted.
    when(financialEventStore.findPrincipalAffectingEvents(DEBT_ID))
        .thenReturn(Collections.emptyList());

    List<InterestPeriod> periods =
        service.calculatePeriodicInterest(
            DEBT_ID, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), ANNUAL_RATE_10_PCT);

    assertThat(periods).isEmpty();
  }

  @Test
  void givenNegativeBalancePeriod_whenCalculateInterest_thenNegativeBalancePeriodExcluded() {
    // Ref: InterestAccrualServiceImpl — balance <= 0 → period skipped.
    // An over-correction drives balance negative; only the positive segment before it is included.
    LocalDate from = LocalDate.of(2025, 1, 1);
    LocalDate corrDate = LocalDate.of(2025, 6, 1);
    LocalDate to = LocalDate.of(2025, 12, 31);

    FinancialEvent debt = buildEvent(EventType.DEBT_REGISTERED, from, new BigDecimal("10000"));
    FinancialEvent overCorrection = buildEvent(EventType.CORRECTION, corrDate, new BigDecimal("-15000"));

    when(financialEventStore.findPrincipalAffectingEvents(DEBT_ID))
        .thenReturn(List.of(debt, overCorrection));

    List<InterestPeriod> periods =
        service.calculatePeriodicInterest(DEBT_ID, from, to, ANNUAL_RATE_10_PCT);

    // Period before the over-correction (balance=10000 > 0) is included.
    // Period after (balance=-5000 <= 0) is excluded.
    assertThat(periods).hasSize(1);
    assertThat(periods.get(0).getPrincipalBalance()).isEqualByComparingTo("10000");
    assertThat(periods.get(0).getPeriodEnd()).isEqualTo(corrDate);
  }

  // ---------------------------------------------------------------------------
  // Exact amount — 365-day basis
  // ---------------------------------------------------------------------------

  @Test
  void givenSingleFullYearPeriod_whenCalculateInterest_thenExactExpectedAmount() {
    // Ref: InterestAccrualServiceImpl — dailyRate = annualRate / 365 × days × principal.
    // 10 000 kr × 10% × 365 days / 365 = exactly 1 000.00 kr.
    LocalDate from = LocalDate.of(2025, 1, 1);
    LocalDate to = LocalDate.of(2026, 1, 1); // 365 days

    FinancialEvent debt = buildEvent(EventType.DEBT_REGISTERED, from, new BigDecimal("10000"));
    when(financialEventStore.findPrincipalAffectingEvents(DEBT_ID)).thenReturn(List.of(debt));

    List<InterestPeriod> periods =
        service.calculatePeriodicInterest(DEBT_ID, from, to, ANNUAL_RATE_10_PCT);

    assertThat(periods).hasSize(1);
    InterestPeriod p = periods.get(0);
    assertThat(p.getDays()).isEqualTo(365L);
    // 10000 × (0.10/365 @ scale 10) × 365 = 999.999990 → rounded HALF_UP to 2dp → 1000.00
    assertThat(p.getInterestAmount()).isEqualByComparingTo("1000.00");
  }

  @Test
  void given365DayYear_whenCalculateInterest_thenDailyRateIsAnnualRateDividedBy365() {
    // Ref: InterestAccrualServiceImpl — DAYS_PER_YEAR = 365 (constant).
    // Uses 1-day period with principal=365 000 and rate=36.5% so that
    // dailyRate = 0.365/365 = 0.001 exactly → interest = 365 000 × 0.001 × 1 = 365.00.
    LocalDate from = LocalDate.of(2025, 3, 1);
    LocalDate to = from.plusDays(1);
    BigDecimal principal = new BigDecimal("365000");
    BigDecimal rate = new BigDecimal("0.365"); // 36.5%

    FinancialEvent debt = buildEvent(EventType.DEBT_REGISTERED, from, principal);
    when(financialEventStore.findPrincipalAffectingEvents(DEBT_ID)).thenReturn(List.of(debt));

    List<InterestPeriod> periods = service.calculatePeriodicInterest(DEBT_ID, from, to, rate);

    assertThat(periods).hasSize(1);
    // dailyRate = 0.365/365 = 0.001 → 365000 × 0.001 × 1 = 365.00
    assertThat(periods.get(0).getInterestAmount()).isEqualByComparingTo("365.00");
  }

  @Test
  void givenKnownPrincipalRateDays_whenCalculateInterest_thenInterestMatchesFormulaToTheOere() {
    // Ref: InterestAccrualServiceImpl — precision test using real Danish debt scenario.
    // principal=50 000, rate=5.75%, period=92 days (Oct 1 – Jan 1).
    // Expected: 50000 × 0.0575/365 × 92 = 725.34... kr → 725.34 kr (HALF_UP).
    LocalDate from = LocalDate.of(2025, 10, 1);
    LocalDate to = LocalDate.of(2026, 1, 1); // 92 days
    BigDecimal principal = new BigDecimal("50000");
    BigDecimal rate = new BigDecimal("0.0575");

    FinancialEvent debt = buildEvent(EventType.DEBT_REGISTERED, from, principal);
    when(financialEventStore.findPrincipalAffectingEvents(DEBT_ID)).thenReturn(List.of(debt));

    List<InterestPeriod> periods = service.calculatePeriodicInterest(DEBT_ID, from, to, rate);

    assertThat(periods).hasSize(1);
    long days = ChronoUnit.DAYS.between(from, to);
    BigDecimal expected =
        principal
            .multiply(rate)
            .divide(new BigDecimal("365"), 10, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(days))
            .setScale(2, RoundingMode.HALF_UP);
    assertThat(periods.get(0).getInterestAmount()).isEqualByComparingTo(expected);
  }

  // ---------------------------------------------------------------------------
  // Multiple periods with balance changes
  // ---------------------------------------------------------------------------

  @Test
  void givenPaymentMidPeriod_whenCalculateInterest_thenTwoPeriodsWithCorrectPrincipals() {
    // Ref: InterestAccrualServiceImpl — payment creates a new balance segment.
    LocalDate from = LocalDate.of(2025, 10, 1);
    LocalDate payDate = LocalDate.of(2025, 11, 15);
    LocalDate to = LocalDate.of(2026, 1, 1);

    FinancialEvent debt = buildEvent(EventType.DEBT_REGISTERED, from, new BigDecimal("50000"));
    FinancialEvent payment = buildEvent(EventType.PAYMENT_RECEIVED, payDate, new BigDecimal("20000"));

    when(financialEventStore.findPrincipalAffectingEvents(DEBT_ID))
        .thenReturn(List.of(debt, payment));

    List<InterestPeriod> periods =
        service.calculatePeriodicInterest(DEBT_ID, from, to, ANNUAL_RATE_10_PCT);

    assertThat(periods).hasSize(2);

    // Period 1: full principal, from → payDate
    assertThat(periods.get(0).getPrincipalBalance()).isEqualByComparingTo("50000");
    assertThat(periods.get(0).getPeriodStart()).isEqualTo(from);
    assertThat(periods.get(0).getPeriodEnd()).isEqualTo(payDate);

    // Period 2: reduced principal, payDate → to
    assertThat(periods.get(1).getPrincipalBalance()).isEqualByComparingTo("30000");
    assertThat(periods.get(1).getPeriodStart()).isEqualTo(payDate);
    assertThat(periods.get(1).getPeriodEnd()).isEqualTo(to);
  }

  @Test
  void givenThreeBalanceSegments_whenCalculateInterest_thenEachSegmentInterestCorrect() {
    // Ref: InterestAccrualServiceImpl — each segment uses its own principalBalance.
    // Verifies segment-level correctness, not just totals.
    LocalDate from = LocalDate.of(2025, 10, 1);
    LocalDate pay1 = LocalDate.of(2025, 11, 1); // 31 days from from
    LocalDate pay2 = LocalDate.of(2025, 12, 1); // 30 days from pay1
    LocalDate to = LocalDate.of(2026, 1, 1); // 31 days from pay2

    FinancialEvent debt = buildEvent(EventType.DEBT_REGISTERED, from, new BigDecimal("100000"));
    FinancialEvent p1 = buildEvent(EventType.PAYMENT_RECEIVED, pay1, new BigDecimal("30000"));
    FinancialEvent p2 = buildEvent(EventType.PAYMENT_RECEIVED, pay2, new BigDecimal("20000"));

    when(financialEventStore.findPrincipalAffectingEvents(DEBT_ID))
        .thenReturn(List.of(debt, p1, p2));

    List<InterestPeriod> periods =
        service.calculatePeriodicInterest(DEBT_ID, from, to, ANNUAL_RATE_10_PCT);

    assertThat(periods).hasSize(3);
    assertThat(periods.get(0).getPrincipalBalance()).isEqualByComparingTo("100000");
    assertThat(periods.get(1).getPrincipalBalance()).isEqualByComparingTo("70000");
    assertThat(periods.get(2).getPrincipalBalance()).isEqualByComparingTo("50000");

    // Each segment's interest is independently correct (not just the total)
    for (InterestPeriod p : periods) {
      BigDecimal expected =
          p.getPrincipalBalance()
              .multiply(ANNUAL_RATE_10_PCT)
              .divide(new BigDecimal("365"), 10, RoundingMode.HALF_UP)
              .multiply(BigDecimal.valueOf(p.getDays()))
              .setScale(2, RoundingMode.HALF_UP);
      assertThat(p.getInterestAmount())
          .as("Interest for principal=%s days=%d", p.getPrincipalBalance(), p.getDays())
          .isEqualByComparingTo(expected);
    }
  }

  // ---------------------------------------------------------------------------
  // Period boundary semantics
  // ---------------------------------------------------------------------------

  @Test
  void givenEventOnFromDate_whenCalculateInterest_thenEventIncludedFromStartOfPeriod() {
    // Ref: InterestAccrualServiceImpl — event.effectiveDate == fromDate is included (not skipped).
    LocalDate from = LocalDate.of(2025, 6, 1);
    LocalDate to = LocalDate.of(2025, 9, 1); // 92 days

    FinancialEvent debt = buildEvent(EventType.DEBT_REGISTERED, from, new BigDecimal("20000"));
    when(financialEventStore.findPrincipalAffectingEvents(DEBT_ID)).thenReturn(List.of(debt));

    List<InterestPeriod> periods =
        service.calculatePeriodicInterest(DEBT_ID, from, to, ANNUAL_RATE_10_PCT);

    assertThat(periods).hasSize(1);
    assertThat(periods.get(0).getPeriodStart()).isEqualTo(from);
    assertThat(periods.get(0).getPrincipalBalance()).isEqualByComparingTo("20000");
  }

  @Test
  void givenFullPaymentOnExactToDate_whenCalculateInterest_thenOnlyOnePeriod() {
    // Ref: InterestAccrualServiceImpl — payment on toDate ends the period; no zero-balance period
    // afterwards.
    LocalDate from = LocalDate.of(2025, 10, 1);
    LocalDate to = LocalDate.of(2026, 1, 1);

    FinancialEvent debt = buildEvent(EventType.DEBT_REGISTERED, from, new BigDecimal("10000"));
    FinancialEvent fullPayment = buildEvent(EventType.PAYMENT_RECEIVED, to, new BigDecimal("10000"));

    when(financialEventStore.findPrincipalAffectingEvents(DEBT_ID))
        .thenReturn(List.of(debt, fullPayment));

    List<InterestPeriod> periods =
        service.calculatePeriodicInterest(DEBT_ID, from, to, ANNUAL_RATE_10_PCT);

    // toDate = periodEnd for the first (and only) period; payment at toDate adds a zero-balance
    // segment beyond toDate which lies outside the calculation window.
    assertThat(periods).hasSize(1);
    assertThat(periods.get(0).getPrincipalBalance()).isEqualByComparingTo("10000");
  }

  @Test
  void givenFullPaymentMidPeriod_whenCalculateInterest_thenZeroBalancePeriodExcluded() {
    // Ref: InterestAccrualServiceImpl — zero balance after payment is not emitted.
    LocalDate from = LocalDate.of(2025, 10, 1);
    LocalDate payDate = LocalDate.of(2025, 11, 1);
    LocalDate to = LocalDate.of(2026, 1, 1);

    FinancialEvent debt = buildEvent(EventType.DEBT_REGISTERED, from, new BigDecimal("10000"));
    FinancialEvent fullPayment =
        buildEvent(EventType.PAYMENT_RECEIVED, payDate, new BigDecimal("10000"));

    when(financialEventStore.findPrincipalAffectingEvents(DEBT_ID))
        .thenReturn(List.of(debt, fullPayment));

    List<InterestPeriod> periods =
        service.calculatePeriodicInterest(DEBT_ID, from, to, ANNUAL_RATE_10_PCT);

    assertThat(periods).hasSize(1);
    assertThat(periods.get(0).getPeriodEnd()).isEqualTo(payDate);
  }

  // ---------------------------------------------------------------------------
  // Zero interest rate
  // ---------------------------------------------------------------------------

  @Test
  void givenZeroAnnualRate_whenCalculateInterest_thenZeroInterestForAllPeriods() {
    // Ref: InterestAccrualServiceImpl — annualRate=0 ÷ 365 = 0 daily rate → 0 interest.
    // Periods ARE still emitted (balance > 0 check passes); their interestAmount is 0.00.
    LocalDate from = LocalDate.of(2025, 1, 1);
    LocalDate to = LocalDate.of(2025, 12, 31);
    BigDecimal zeroRate = BigDecimal.ZERO;

    FinancialEvent debt = buildEvent(EventType.DEBT_REGISTERED, from, new BigDecimal("50000"));
    when(financialEventStore.findPrincipalAffectingEvents(DEBT_ID)).thenReturn(List.of(debt));

    List<InterestPeriod> periods = service.calculatePeriodicInterest(DEBT_ID, from, to, zeroRate);

    assertThat(periods).isNotEmpty();
    periods.forEach(
        p ->
            assertThat(p.getInterestAmount())
                .as("All periods must have zero interest when rate=0")
                .isEqualByComparingTo(BigDecimal.ZERO));
  }

  // ---------------------------------------------------------------------------
  // REFUND and UDLAEG_CORRECTED balance effects
  // ---------------------------------------------------------------------------

  @Test
  void givenUdlaegCorrectionNegativeDelta_whenCalculateInterest_thenPrincipalReducedForSubsequentPeriod() {
    // Ref: InterestAccrualServiceImpl — UDLAEG_CORRECTED adds delta (may be negative).
    LocalDate from = LocalDate.of(2025, 10, 1);
    LocalDate corrDate = LocalDate.of(2025, 12, 1);
    LocalDate to = LocalDate.of(2026, 3, 1);

    FinancialEvent debt = buildEvent(EventType.DEBT_REGISTERED, from, new BigDecimal("50000"));
    FinancialEvent correction =
        buildEvent(EventType.UDLAEG_CORRECTED, corrDate, new BigDecimal("-20000"));

    when(financialEventStore.findPrincipalAffectingEvents(DEBT_ID))
        .thenReturn(List.of(debt, correction));

    List<InterestPeriod> periods =
        service.calculatePeriodicInterest(DEBT_ID, from, to, ANNUAL_RATE_10_PCT);

    assertThat(periods).hasSize(2);
    // Before correction: full principal
    assertThat(periods.get(0).getPrincipalBalance()).isEqualByComparingTo("50000");
    assertThat(periods.get(0).getPeriodEnd()).isEqualTo(corrDate);
    // After correction: reduced principal (50 000 − 20 000 = 30 000)
    assertThat(periods.get(1).getPrincipalBalance()).isEqualByComparingTo("30000");
    assertThat(periods.get(1).getPeriodStart()).isEqualTo(corrDate);
  }

  @Test
  void givenRefundEvent_whenCalculateInterest_thenPrincipalIncreasedForSubsequentPeriod() {
    // Ref: InterestAccrualServiceImpl — REFUND increases running balance.
    LocalDate from = LocalDate.of(2025, 1, 1);
    LocalDate refundDate = LocalDate.of(2025, 6, 1);
    LocalDate to = LocalDate.of(2026, 1, 1);
    BigDecimal originalDebt = new BigDecimal("10000");
    BigDecimal refundAmount = new BigDecimal("5000");

    FinancialEvent debt = buildEvent(EventType.DEBT_REGISTERED, from, originalDebt);
    FinancialEvent refund = buildEvent(EventType.REFUND, refundDate, refundAmount);

    when(financialEventStore.findPrincipalAffectingEvents(DEBT_ID))
        .thenReturn(List.of(debt, refund));

    List<InterestPeriod> periods =
        service.calculatePeriodicInterest(DEBT_ID, from, to, ANNUAL_RATE_10_PCT);

    assertThat(periods).hasSize(2);
    assertThat(periods.get(0).getPrincipalBalance()).isEqualByComparingTo("10000");
    // After refund: principal = 10 000 + 5 000 = 15 000
    assertThat(periods.get(1).getPrincipalBalance()).isEqualByComparingTo("15000");
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private FinancialEvent buildEvent(EventType type, LocalDate effectiveDate, BigDecimal amount) {
    return FinancialEvent.builder()
        .id(UUID.randomUUID())
        .debtId(DEBT_ID)
        .eventType(type)
        .effectiveDate(effectiveDate)
        .amount(amount)
        .build();
  }
}
