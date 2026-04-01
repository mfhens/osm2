package dk.ufst.bookkeeping.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import dk.ufst.bookkeeping.domain.FinancialEvent;
import dk.ufst.bookkeeping.model.InterestPeriod;
import dk.ufst.bookkeeping.port.FinancialEventStore;
import dk.ufst.bookkeeping.service.InterestAccrualService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class InterestAccrualServiceImpl implements InterestAccrualService {

  private static final BigDecimal DAYS_PER_YEAR = new BigDecimal("365");
  private static final int CALCULATION_SCALE = 10;

  private final FinancialEventStore financialEventStore;

  @Override
  public List<InterestPeriod> calculatePeriodicInterest(
      UUID debtId, LocalDate fromDate, LocalDate toDate, BigDecimal annualRate) {

    log.info(
        "Calculating periodic interest for debt={}, from={}, to={}, rate={}",
        debtId,
        fromDate,
        toDate,
        annualRate);

    List<FinancialEvent> events = financialEventStore.findPrincipalAffectingEvents(debtId);
    List<BalanceChange> balanceTimeline = buildBalanceTimeline(events, fromDate);
    return calculateInterestPerPeriod(balanceTimeline, fromDate, toDate, annualRate);
  }

  private List<BalanceChange> buildBalanceTimeline(
      List<FinancialEvent> events, LocalDate fromDate) {

    List<BalanceChange> timeline = new ArrayList<>();
    BigDecimal runningBalance = BigDecimal.ZERO;

    for (FinancialEvent event : events) {
      switch (event.getEventType()) {
        case DEBT_REGISTERED, UDLAEG_REGISTERED:
          runningBalance = runningBalance.add(event.getAmount());
          break;
        case PAYMENT_RECEIVED, OFFSETTING_EXECUTED, WRITE_OFF:
          runningBalance = runningBalance.subtract(event.getAmount());
          break;
        case UDLAEG_CORRECTED, CORRECTION:
          // Amount is the delta (positive = increase, negative = decrease)
          runningBalance = runningBalance.add(event.getAmount());
          break;
        case REFUND:
          runningBalance = runningBalance.add(event.getAmount());
          break;
        default:
          break;
      }

      if (!event.getEffectiveDate().isBefore(fromDate)) {
        timeline.add(new BalanceChange(event.getEffectiveDate(), runningBalance));
      }
    }

    if (timeline.isEmpty() || timeline.get(0).date().isAfter(fromDate)) {
      timeline.add(0, new BalanceChange(fromDate, getBalanceAtDate(events, fromDate)));
    }

    return timeline;
  }

  private BigDecimal getBalanceAtDate(List<FinancialEvent> events, LocalDate date) {
    BigDecimal balance = BigDecimal.ZERO;
    for (FinancialEvent event : events) {
      if (event.getEffectiveDate().isAfter(date)) {
        break;
      }
      switch (event.getEventType()) {
        case DEBT_REGISTERED, UDLAEG_REGISTERED:
          balance = balance.add(event.getAmount());
          break;
        case PAYMENT_RECEIVED, OFFSETTING_EXECUTED, WRITE_OFF:
          balance = balance.subtract(event.getAmount());
          break;
        case UDLAEG_CORRECTED, CORRECTION:
          balance = balance.add(event.getAmount());
          break;
        case REFUND:
          balance = balance.add(event.getAmount());
          break;
        default:
          break;
      }
    }
    return balance;
  }

  private List<InterestPeriod> calculateInterestPerPeriod(
      List<BalanceChange> timeline, LocalDate fromDate, LocalDate toDate, BigDecimal annualRate) {

    List<InterestPeriod> periods = new ArrayList<>();
    BigDecimal dailyRate =
        annualRate.divide(DAYS_PER_YEAR, CALCULATION_SCALE, RoundingMode.HALF_UP);

    for (int i = 0; i < timeline.size(); i++) {
      BalanceChange current = timeline.get(i);
      LocalDate periodStart = current.date().isBefore(fromDate) ? fromDate : current.date();
      LocalDate periodEnd = (i + 1 < timeline.size()) ? timeline.get(i + 1).date() : toDate;

      if (!periodStart.isBefore(periodEnd)) {
        continue;
      }

      if (current.balance().compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }

      long days = ChronoUnit.DAYS.between(periodStart, periodEnd);
      BigDecimal interest =
          current
              .balance()
              .multiply(dailyRate)
              .multiply(BigDecimal.valueOf(days))
              .setScale(2, RoundingMode.HALF_UP);

      InterestPeriod period =
          InterestPeriod.builder()
              .periodStart(periodStart)
              .periodEnd(periodEnd)
              .principalBalance(current.balance())
              .annualRate(annualRate)
              .days(days)
              .interestAmount(interest)
              .build();

      periods.add(period);

      log.debug(
          "Interest period: {} to {}, principal={}, days={}, interest={}",
          periodStart,
          periodEnd,
          current.balance(),
          days,
          interest);
    }

    return periods;
  }

  private record BalanceChange(LocalDate date, BigDecimal balance) {}
}
