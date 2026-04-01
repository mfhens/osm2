package dk.ufst.bookkeeping.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import dk.ufst.bookkeeping.domain.EntryCategory;
import dk.ufst.bookkeeping.domain.EntryType;
import dk.ufst.bookkeeping.domain.EventType;
import dk.ufst.bookkeeping.domain.FinancialEvent;
import dk.ufst.bookkeeping.domain.LedgerEntry;
import dk.ufst.bookkeeping.model.CoverageAllocation;
import dk.ufst.bookkeeping.model.CoverageReversal;
import dk.ufst.bookkeeping.model.InterestPeriod;
import dk.ufst.bookkeeping.model.TimelineReplayResult;
import dk.ufst.bookkeeping.port.CoveragePriorityPort;
import dk.ufst.bookkeeping.port.FinancialEventStore;
import dk.ufst.bookkeeping.port.LedgerEntryStore;
import dk.ufst.bookkeeping.service.EventOrderComparator;
import dk.ufst.bookkeeping.service.TimelineReplayService;
import dk.ufst.bookkeeping.spi.Kontoplan;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class TimelineReplayServiceImpl implements TimelineReplayService {

  private static final BigDecimal DAYS_PER_YEAR = new BigDecimal("365");
  private static final int CALC_SCALE = 10;

  private final FinancialEventStore financialEventStore;
  private final LedgerEntryStore ledgerEntryStore;
  private final CoveragePriorityPort coveragePriorityPort;
  private final Kontoplan kontoplan;

  @Override
  public TimelineReplayResult replayTimeline(
      UUID debtId,
      LocalDate crossingPoint,
      BigDecimal annualInterestRate,
      String triggeringReference) {

    log.info(
        "TIMELINE REPLAY: debtId={}, crossingPoint={}, rate={}",
        debtId,
        crossingPoint,
        annualInterestRate);

    // 1. Storno all ledger entries from crossing point forward
    int stornoCount = stornoEntriesFromDate(debtId, crossingPoint, triggeringReference);

    // 2. Load all events and sort deterministically
    List<FinancialEvent> allEvents =
        new ArrayList<>(
            financialEventStore.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(debtId));
    allEvents.sort(EventOrderComparator.INSTANCE);

    // 3. Replay: walk the timeline computing balances and interest periods
    LocalDate replayEnd = LocalDate.now();
    ReplayState state = new ReplayState();
    List<InterestPeriod> interestPeriods = new ArrayList<>();
    List<CoverageAllocation> allocations = new ArrayList<>();
    List<CoverageReversal> reversals = new ArrayList<>();

    // Build balance up to crossing point first
    for (FinancialEvent event : allEvents) {
      if (event.getEffectiveDate().isBefore(crossingPoint)) {
        applyEventToState(event, state);
      }
    }

    // Collect events from crossing point, grouped by date for interest calculation
    List<FinancialEvent> replayEvents =
        allEvents.stream()
            .filter(
                e ->
                    !e.getEffectiveDate().isBefore(crossingPoint)
                        && !e.getEffectiveDate().isAfter(replayEnd))
            .filter(e -> e.getEventType() != EventType.INTEREST_ACCRUED)
            .filter(e -> e.getEventType() != EventType.COVERAGE_REVERSED)
            .toList();

    // 4. Walk timeline: for each event, compute interest from previous point, then apply event
    LocalDate previousDate = crossingPoint;
    int newEntryCount = 0;

    for (FinancialEvent event : replayEvents) {
      LocalDate eventDate = event.getEffectiveDate();

      // Calculate interest for the gap between previous date and this event
      InterestAccrualResult gap =
          accrueInterestForPeriod(
              debtId,
              previousDate,
              eventDate,
              state.principalBalance,
              annualInterestRate,
              interestPeriods,
              triggeringReference);
      state.accruedInterest = state.accruedInterest.add(gap.interestAccrued());
      newEntryCount += gap.entriesPosted();

      // Apply the event with coverage priority if it is a payment/recovery
      if (isRecoveryEvent(event)) {
        CoverageAllocation allocation =
            coveragePriorityPort.allocatePayment(
                debtId,
                event.getAmount(),
                state.accruedInterest,
                BigDecimal.ZERO,
                state.principalBalance);
        allocation.setEffectiveDate(eventDate);
        allocation.setSourceEventId(event.getId());
        allocations.add(allocation);

        // Check if allocation changed vs original (detect dækningsophævelse)
        checkForCoverageReversal(event, allocation, state, reversals, triggeringReference);

        state.accruedInterest = state.accruedInterest.subtract(allocation.getInterestPortion());
        state.principalBalance = state.principalBalance.subtract(allocation.getPrincipalPortion());
        newEntryCount += postRecoveryEntries(debtId, eventDate, allocation, event.getReference());
      } else {
        applyEventToState(event, state);
        newEntryCount += repostEventEntries(debtId, event);
      }

      previousDate = eventDate;
    }

    // Final interest period from last event to today
    InterestAccrualResult finalGap =
        accrueInterestForPeriod(
            debtId,
            previousDate,
            replayEnd,
            state.principalBalance,
            annualInterestRate,
            interestPeriods,
            triggeringReference);
    state.accruedInterest = state.accruedInterest.add(finalGap.interestAccrued());
    newEntryCount += finalGap.entriesPosted();

    BigDecimal newInterestTotal =
        interestPeriods.stream()
            .map(InterestPeriod::getInterestAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    log.info(
        "TIMELINE REPLAY COMPLETE: debtId={}, storno={}, newEntries={}, "
            + "interestPeriods={}, reversals={}, finalPrincipal={}, finalInterest={}",
        debtId,
        stornoCount,
        newEntryCount,
        interestPeriods.size(),
        reversals.size(),
        state.principalBalance,
        state.accruedInterest);

    return TimelineReplayResult.builder()
        .debtId(debtId)
        .replayFromDate(crossingPoint)
        .replayToDate(replayEnd)
        .stornoEntriesPosted(stornoCount)
        .newEntriesPosted(newEntryCount)
        .recalculatedInterestPeriods(interestPeriods)
        .recalculatedAllocations(allocations)
        .coverageReversals(reversals)
        .newInterestTotal(newInterestTotal)
        .finalPrincipalBalance(state.principalBalance)
        .finalInterestBalance(state.accruedInterest)
        .build();
  }

  private int stornoEntriesFromDate(UUID debtId, LocalDate fromDate, String reason) {
    List<LedgerEntry> activeEntries =
        ledgerEntryStore.findActiveEntriesByDebtId(debtId).stream()
            .filter(e -> !e.getEffectiveDate().isBefore(fromDate))
            .toList();

    Set<UUID> transactionIds = new LinkedHashSet<>();
    for (LedgerEntry entry : activeEntries) {
      transactionIds.add(entry.getTransactionId());
    }

    int count = 0;
    LocalDate today = LocalDate.now();

    for (UUID originalTxnId : transactionIds) {
      if (ledgerEntryStore.existsByReversalOfTransactionId(originalTxnId)) {
        continue;
      }

      List<LedgerEntry> txnEntries = ledgerEntryStore.findByTransactionId(originalTxnId);
      UUID stornoTxnId = UUID.randomUUID();

      for (LedgerEntry original : txnEntries) {
        EntryType reversedType =
            original.getEntryType() == EntryType.DEBIT ? EntryType.CREDIT : EntryType.DEBIT;

        LedgerEntry stornoEntry =
            LedgerEntry.builder()
                .transactionId(stornoTxnId)
                .debtId(debtId)
                .accountCode(original.getAccountCode())
                .accountName(original.getAccountName())
                .entryType(reversedType)
                .amount(original.getAmount())
                .effectiveDate(original.getEffectiveDate())
                .postingDate(today)
                .reference(original.getReference())
                .description("STORNO (crossing): " + reason)
                .reversalOfTransactionId(originalTxnId)
                .entryCategory(EntryCategory.STORNO)
                .build();

        ledgerEntryStore.saveSingle(stornoEntry);
        count++;
      }
    }

    log.info(
        "STORNO: reversed {} entries across {} transactions from {} for debtId={}",
        count,
        transactionIds.size(),
        fromDate,
        debtId);
    return count;
  }

  private InterestPeriod calculateInterestForPeriod(
      LocalDate from, LocalDate to, BigDecimal principal, BigDecimal annualRate) {

    long days = ChronoUnit.DAYS.between(from, to);
    BigDecimal dailyRate = annualRate.divide(DAYS_PER_YEAR, CALC_SCALE, RoundingMode.HALF_UP);
    BigDecimal interest =
        principal
            .multiply(dailyRate)
            .multiply(BigDecimal.valueOf(days))
            .setScale(2, RoundingMode.HALF_UP);

    return InterestPeriod.builder()
        .periodStart(from)
        .periodEnd(to)
        .principalBalance(principal)
        .annualRate(annualRate)
        .days(days)
        .interestAmount(interest)
        .build();
  }

  private int postInterestEntries(UUID debtId, InterestPeriod period, String reference) {
    UUID txnId = UUID.randomUUID();
    LocalDate today = LocalDate.now();
    String desc =
        String.format(
            "Rente recalc: %s to %s, principal=%s, days=%d",
            period.getPeriodStart(),
            period.getPeriodEnd(),
            period.getPrincipalBalance().toPlainString(),
            period.getDays());

    ledgerEntryStore.saveSingle(
        LedgerEntry.builder()
            .transactionId(txnId)
            .debtId(debtId)
            .accountCode(kontoplan.interestReceivable().getCode())
            .accountName(kontoplan.interestReceivable().getName())
            .entryType(EntryType.DEBIT)
            .amount(period.getInterestAmount())
            .effectiveDate(period.getPeriodStart())
            .postingDate(today)
            .reference(reference)
            .description(desc)
            .entryCategory(EntryCategory.INTEREST_ACCRUAL)
            .build());

    ledgerEntryStore.saveSingle(
        LedgerEntry.builder()
            .transactionId(txnId)
            .debtId(debtId)
            .accountCode(kontoplan.interestRevenue().getCode())
            .accountName(kontoplan.interestRevenue().getName())
            .entryType(EntryType.CREDIT)
            .amount(period.getInterestAmount())
            .effectiveDate(period.getPeriodStart())
            .postingDate(today)
            .reference(reference)
            .description(desc)
            .entryCategory(EntryCategory.INTEREST_ACCRUAL)
            .build());

    return 2;
  }

  private int postRecoveryEntries(
      UUID debtId, LocalDate effectiveDate, CoverageAllocation allocation, String reference) {

    int count = 0;
    UUID txnId = UUID.randomUUID();
    LocalDate today = LocalDate.now();

    // Interest portion: debit bank, credit interest receivable
    if (allocation.getInterestPortion().compareTo(BigDecimal.ZERO) > 0) {
      ledgerEntryStore.saveSingle(
          LedgerEntry.builder()
              .transactionId(txnId)
              .debtId(debtId)
              .accountCode(kontoplan.bank().getCode())
              .accountName(kontoplan.bank().getName())
              .entryType(EntryType.DEBIT)
              .amount(allocation.getInterestPortion())
              .effectiveDate(effectiveDate)
              .postingDate(today)
              .reference(reference)
              .description("Daekning: rente")
              .entryCategory(EntryCategory.PAYMENT)
              .build());
      ledgerEntryStore.saveSingle(
          LedgerEntry.builder()
              .transactionId(txnId)
              .debtId(debtId)
              .accountCode(kontoplan.interestReceivable().getCode())
              .accountName(kontoplan.interestReceivable().getName())
              .entryType(EntryType.CREDIT)
              .amount(allocation.getInterestPortion())
              .effectiveDate(effectiveDate)
              .postingDate(today)
              .reference(reference)
              .description("Daekning: rente")
              .entryCategory(EntryCategory.PAYMENT)
              .build());
      count += 2;
    }

    // Principal portion: debit bank, credit receivables
    if (allocation.getPrincipalPortion().compareTo(BigDecimal.ZERO) > 0) {
      UUID txnId2 = UUID.randomUUID();
      ledgerEntryStore.saveSingle(
          LedgerEntry.builder()
              .transactionId(txnId2)
              .debtId(debtId)
              .accountCode(kontoplan.bank().getCode())
              .accountName(kontoplan.bank().getName())
              .entryType(EntryType.DEBIT)
              .amount(allocation.getPrincipalPortion())
              .effectiveDate(effectiveDate)
              .postingDate(today)
              .reference(reference)
              .description("Daekning: hovedstol")
              .entryCategory(EntryCategory.PAYMENT)
              .build());
      ledgerEntryStore.saveSingle(
          LedgerEntry.builder()
              .transactionId(txnId2)
              .debtId(debtId)
              .accountCode(kontoplan.receivables().getCode())
              .accountName(kontoplan.receivables().getName())
              .entryType(EntryType.CREDIT)
              .amount(allocation.getPrincipalPortion())
              .effectiveDate(effectiveDate)
              .postingDate(today)
              .reference(reference)
              .description("Daekning: hovedstol")
              .entryCategory(EntryCategory.PAYMENT)
              .build());
      count += 2;
    }

    return count;
  }

  private int repostEventEntries(UUID debtId, FinancialEvent event) {
    UUID txnId = UUID.randomUUID();
    LocalDate today = LocalDate.now();

    String debitCode;
    String debitName;
    String creditCode;
    String creditName;
    EntryCategory category;

    switch (event.getEventType()) {
      case DEBT_REGISTERED, UDLAEG_REGISTERED -> {
        debitCode = kontoplan.receivables().getCode();
        debitName = kontoplan.receivables().getName();
        creditCode = kontoplan.collectionRevenue().getCode();
        creditName = kontoplan.collectionRevenue().getName();
        category = EntryCategory.DEBT_REGISTRATION;
      }
      case OFFSETTING_EXECUTED -> {
        debitCode = kontoplan.offsettingClearing().getCode();
        debitName = kontoplan.offsettingClearing().getName();
        creditCode = kontoplan.receivables().getCode();
        creditName = kontoplan.receivables().getName();
        category = EntryCategory.OFFSETTING;
      }
      case WRITE_OFF -> {
        debitCode = kontoplan.writeOffExpense().getCode();
        debitName = kontoplan.writeOffExpense().getName();
        creditCode = kontoplan.receivables().getCode();
        creditName = kontoplan.receivables().getName();
        category = EntryCategory.WRITE_OFF;
      }
      case REFUND -> {
        debitCode = kontoplan.receivables().getCode();
        debitName = kontoplan.receivables().getName();
        creditCode = kontoplan.bank().getCode();
        creditName = kontoplan.bank().getName();
        category = EntryCategory.REFUND;
      }
      case UDLAEG_CORRECTED, CORRECTION -> {
        if (event.getAmount().compareTo(BigDecimal.ZERO) >= 0) {
          debitCode = kontoplan.receivables().getCode();
          debitName = kontoplan.receivables().getName();
          creditCode = kontoplan.collectionRevenue().getCode();
          creditName = kontoplan.collectionRevenue().getName();
        } else {
          debitCode = kontoplan.collectionRevenue().getCode();
          debitName = kontoplan.collectionRevenue().getName();
          creditCode = kontoplan.receivables().getCode();
          creditName = kontoplan.receivables().getName();
        }
        category = EntryCategory.CORRECTION;
      }
      default -> {
        return 0;
      }
    }

    BigDecimal amount = event.getAmount().abs();

    ledgerEntryStore.saveSingle(
        LedgerEntry.builder()
            .transactionId(txnId)
            .debtId(debtId)
            .accountCode(debitCode)
            .accountName(debitName)
            .entryType(EntryType.DEBIT)
            .amount(amount)
            .effectiveDate(event.getEffectiveDate())
            .postingDate(today)
            .reference(event.getReference())
            .description("REPLAY: " + event.getDescription())
            .entryCategory(category)
            .build());

    ledgerEntryStore.saveSingle(
        LedgerEntry.builder()
            .transactionId(txnId)
            .debtId(debtId)
            .accountCode(creditCode)
            .accountName(creditName)
            .entryType(EntryType.CREDIT)
            .amount(amount)
            .effectiveDate(event.getEffectiveDate())
            .postingDate(today)
            .reference(event.getReference())
            .description("REPLAY: " + event.getDescription())
            .entryCategory(category)
            .build());

    return 2;
  }

  private boolean isRecoveryEvent(FinancialEvent event) {
    return event.getEventType() == EventType.PAYMENT_RECEIVED
        || event.getEventType() == EventType.OFFSETTING_EXECUTED;
  }

  private void applyEventToState(FinancialEvent event, ReplayState state) {
    switch (event.getEventType()) {
      case DEBT_REGISTERED, UDLAEG_REGISTERED:
        state.principalBalance = state.principalBalance.add(event.getAmount());
        break;
      case WRITE_OFF:
        state.principalBalance = state.principalBalance.subtract(event.getAmount());
        break;
      case UDLAEG_CORRECTED, CORRECTION:
        state.principalBalance = state.principalBalance.add(event.getAmount());
        break;
      case REFUND:
        state.principalBalance = state.principalBalance.add(event.getAmount());
        break;
      default:
        break;
    }
  }

  private void checkForCoverageReversal(
      FinancialEvent event,
      CoverageAllocation newAllocation,
      ReplayState state,
      List<CoverageReversal> reversals,
      String triggeringReference) {

    if (event.getLedgerTransactionId() != null) {
      List<LedgerEntry> originalEntries =
          ledgerEntryStore.findByTransactionId(event.getLedgerTransactionId());

      if (!originalEntries.isEmpty()) {
        OriginalPortions portions = computeOriginalPortions(originalEntries);
        BigDecimal originalInterestPortion = portions.interestPortion();
        BigDecimal originalPrincipalPortion = portions.principalPortion();
        boolean allocationChanged =
            newAllocation.getInterestPortion().compareTo(originalInterestPortion) != 0
                || newAllocation.getPrincipalPortion().compareTo(originalPrincipalPortion) != 0;

        if (allocationChanged) {
          CoverageAllocation original =
              CoverageAllocation.builder()
                  .debtId(event.getDebtId())
                  .totalAmount(event.getAmount())
                  .interestPortion(originalInterestPortion)
                  .principalPortion(originalPrincipalPortion)
                  .build();

          CoverageReversal reversal =
              CoverageReversal.builder()
                  .debtId(event.getDebtId())
                  .originalTransactionId(event.getLedgerTransactionId())
                  .crossingEventId(event.getId())
                  .effectiveDate(event.getEffectiveDate())
                  .originalAllocation(original)
                  .replacementAllocation(newAllocation)
                  .interestDelta(
                      newAllocation.getInterestPortion().subtract(originalInterestPortion))
                  .principalDelta(
                      newAllocation.getPrincipalPortion().subtract(originalPrincipalPortion))
                  .reason("Daekningsophaevelse: krydsende transaktion " + triggeringReference)
                  .build();

          reversals.add(reversal);

          log.info(
              "DAEKNINGSOPHAEVELSE: debtId={}, eventDate={}, "
                  + "oldInterest={}, newInterest={}, oldPrincipal={}, newPrincipal={}",
              event.getDebtId(),
              event.getEffectiveDate(),
              originalInterestPortion,
              newAllocation.getInterestPortion(),
              originalPrincipalPortion,
              newAllocation.getPrincipalPortion());
        }
      }
    }
  }

  private record InterestAccrualResult(BigDecimal interestAccrued, int entriesPosted) {}

  /**
   * Accrues interest for a date gap if the period is positive and principal is non-zero. Appends
   * the period to {@code interestPeriods} and posts ledger entries.
   */
  private InterestAccrualResult accrueInterestForPeriod(
      UUID debtId,
      LocalDate from,
      LocalDate to,
      BigDecimal principal,
      BigDecimal annualRate,
      List<InterestPeriod> interestPeriods,
      String reference) {
    if (!to.isAfter(from) || principal.compareTo(BigDecimal.ZERO) <= 0) {
      return new InterestAccrualResult(BigDecimal.ZERO, 0);
    }
    InterestPeriod period = calculateInterestForPeriod(from, to, principal, annualRate);
    if (period.getInterestAmount().compareTo(BigDecimal.ZERO) <= 0) {
      return new InterestAccrualResult(BigDecimal.ZERO, 0);
    }
    interestPeriods.add(period);
    return new InterestAccrualResult(
        period.getInterestAmount(), postInterestEntries(debtId, period, reference));
  }

  private record OriginalPortions(BigDecimal interestPortion, BigDecimal principalPortion) {}

  /**
   * Sums CREDIT ledger entries into interest and principal portions for dækningsophævelse
   * detection.
   */
  private OriginalPortions computeOriginalPortions(List<LedgerEntry> entries) {
    BigDecimal interest = BigDecimal.ZERO;
    BigDecimal principal = BigDecimal.ZERO;
    for (LedgerEntry entry : entries) {
      if (entry.getEntryType() == EntryType.CREDIT) {
        if (kontoplan.interestReceivable().getCode().equals(entry.getAccountCode())) {
          interest = interest.add(entry.getAmount());
        } else if (kontoplan.receivables().getCode().equals(entry.getAccountCode())) {
          principal = principal.add(entry.getAmount());
        }
      }
    }
    return new OriginalPortions(interest, principal);
  }

  private static class ReplayState {
    BigDecimal principalBalance = BigDecimal.ZERO;
    BigDecimal accruedInterest = BigDecimal.ZERO;
  }
}
