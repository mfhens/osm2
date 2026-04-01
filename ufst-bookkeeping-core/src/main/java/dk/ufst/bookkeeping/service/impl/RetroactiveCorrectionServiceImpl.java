package dk.ufst.bookkeeping.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import dk.ufst.bookkeeping.domain.EntryCategory;
import dk.ufst.bookkeeping.domain.EntryType;
import dk.ufst.bookkeeping.domain.EventType;
import dk.ufst.bookkeeping.domain.FinancialEvent;
import dk.ufst.bookkeeping.domain.LedgerEntry;
import dk.ufst.bookkeeping.model.CorrectionResult;
import dk.ufst.bookkeeping.model.InterestPeriod;
import dk.ufst.bookkeeping.port.FinancialEventStore;
import dk.ufst.bookkeeping.port.LedgerEntryStore;
import dk.ufst.bookkeeping.service.InterestAccrualService;
import dk.ufst.bookkeeping.service.RetroactiveCorrectionService;
import dk.ufst.bookkeeping.spi.Kontoplan;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class RetroactiveCorrectionServiceImpl implements RetroactiveCorrectionService {

  private static final String CORRECTION_PREFIX = "KORREKTION: ";

  private final LedgerEntryStore ledgerEntryStore;
  private final FinancialEventStore financialEventStore;
  private final InterestAccrualService interestAccrualService;
  private final Kontoplan kontoplan;

  @Override
  public CorrectionResult applyRetroactiveCorrection(
      UUID debtId,
      LocalDate effectiveDate,
      BigDecimal originalAmount,
      BigDecimal correctedAmount,
      BigDecimal annualInterestRate,
      String reference,
      String reason) {

    log.info(
        "CORRECTION: debtId={}, effectiveDate={}, original={}, corrected={}, reason={}",
        debtId,
        effectiveDate,
        originalAmount,
        correctedAmount,
        reason);

    BigDecimal principalDelta = correctedAmount.subtract(originalAmount);

    // 1. Record correction event in timeline
    FinancialEvent correctionEvent =
        recordCorrectionEvent(debtId, effectiveDate, principalDelta, reference, reason);

    // 2. Post the principal correction to the ledger
    postPrincipalCorrection(debtId, effectiveDate, principalDelta, reference, reason);

    // 3. Find and storno all interest accruals after the effective date
    List<LedgerEntry> affectedInterestEntries =
        ledgerEntryStore.findInterestAccrualsAfterDate(debtId, effectiveDate);

    BigDecimal oldInterestTotal = calculateInterestTotal(affectedInterestEntries);
    int stornoCount = stornoInterestEntries(debtId, affectedInterestEntries, reason);

    // 4. Recalculate interest from effective date to today
    LocalDate recalcEndDate = LocalDate.now();
    List<InterestPeriod> newPeriods =
        interestAccrualService.calculatePeriodicInterest(
            debtId, effectiveDate, recalcEndDate, annualInterestRate);

    // 5. Post new interest accrual entries
    int newEntryCount = postRecalculatedInterest(debtId, newPeriods, reference);

    BigDecimal newInterestTotal =
        newPeriods.stream()
            .map(InterestPeriod::getInterestAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal interestDelta = newInterestTotal.subtract(oldInterestTotal);

    log.info(
        "CORRECTION COMPLETE: debtId={}, principalDelta={}, storno={}, newEntries={}, "
            + "oldInterest={}, newInterest={}, interestDelta={}",
        debtId,
        principalDelta,
        stornoCount,
        newEntryCount,
        oldInterestTotal,
        newInterestTotal,
        interestDelta);

    return CorrectionResult.builder()
        .debtId(debtId)
        .correctionEventId(correctionEvent.getId())
        .principalDelta(principalDelta)
        .stornoEntriesPosted(stornoCount)
        .newInterestEntriesPosted(newEntryCount)
        .oldInterestTotal(oldInterestTotal)
        .newInterestTotal(newInterestTotal)
        .interestDelta(interestDelta)
        .recalculatedPeriods(newPeriods)
        .build();
  }

  private FinancialEvent recordCorrectionEvent(
      UUID debtId, LocalDate effectiveDate, BigDecimal delta, String reference, String reason) {

    FinancialEvent event =
        FinancialEvent.builder()
            .debtId(debtId)
            .eventType(EventType.UDLAEG_CORRECTED)
            .effectiveDate(effectiveDate)
            .amount(delta)
            .reference(reference)
            .description(reason)
            .build();

    return financialEventStore.save(event);
  }

  private void postPrincipalCorrection(
      UUID debtId, LocalDate effectiveDate, BigDecimal delta, String reference, String reason) {

    UUID transactionId = UUID.randomUUID();
    LocalDate today = LocalDate.now();

    if (delta.compareTo(BigDecimal.ZERO) < 0) {
      // Principal decreased: reverse the receivable
      BigDecimal absDelta = delta.abs();
      saveLedgerEntry(
          new LedgerEntryRequest(
              transactionId,
              debtId,
              kontoplan.collectionRevenue().getCode(),
              kontoplan.collectionRevenue().getName(),
              EntryType.DEBIT,
              absDelta,
              effectiveDate,
              today,
              reference,
              correctionDescription(reason),
              null,
              EntryCategory.CORRECTION));
      saveLedgerEntry(
          new LedgerEntryRequest(
              transactionId,
              debtId,
              kontoplan.receivables().getCode(),
              kontoplan.receivables().getName(),
              EntryType.CREDIT,
              absDelta,
              effectiveDate,
              today,
              reference,
              correctionDescription(reason),
              null,
              EntryCategory.CORRECTION));
    } else if (delta.compareTo(BigDecimal.ZERO) > 0) {
      // Principal increased
      saveLedgerEntry(
          new LedgerEntryRequest(
              transactionId,
              debtId,
              kontoplan.receivables().getCode(),
              kontoplan.receivables().getName(),
              EntryType.DEBIT,
              delta,
              effectiveDate,
              today,
              reference,
              correctionDescription(reason),
              null,
              EntryCategory.CORRECTION));
      saveLedgerEntry(
          new LedgerEntryRequest(
              transactionId,
              debtId,
              kontoplan.collectionRevenue().getCode(),
              kontoplan.collectionRevenue().getName(),
              EntryType.CREDIT,
              delta,
              effectiveDate,
              today,
              reference,
              correctionDescription(reason),
              null,
              EntryCategory.CORRECTION));
    }
  }

  private int stornoInterestEntries(UUID debtId, List<LedgerEntry> entries, String reason) {

    Set<UUID> transactionIds =
        entries.stream().map(LedgerEntry::getTransactionId).collect(Collectors.toSet());

    int count = 0;
    LocalDate today = LocalDate.now();

    for (UUID originalTxnId : transactionIds) {
      if (ledgerEntryStore.existsByReversalOfTransactionId(originalTxnId)) {
        log.debug("Transaction {} already reversed, skipping", originalTxnId);
        continue;
      }

      List<LedgerEntry> txnEntries = ledgerEntryStore.findByTransactionId(originalTxnId);
      UUID stornoTxnId = UUID.randomUUID();

      for (LedgerEntry original : txnEntries) {
        // Reverse: swap debit/credit; copy account code/name directly (no lookup needed)
        EntryType reversedType =
            original.getEntryType() == EntryType.DEBIT ? EntryType.CREDIT : EntryType.DEBIT;

        saveLedgerEntry(
            new LedgerEntryRequest(
                stornoTxnId,
                debtId,
                original.getAccountCode(),
                original.getAccountName(),
                reversedType,
                original.getAmount(),
                original.getEffectiveDate(),
                today,
                original.getReference(),
                "STORNO: " + reason,
                originalTxnId,
                EntryCategory.STORNO));
        count++;
      }
    }

    log.info("Storno: reversed {} entries across {} transactions", count, transactionIds.size());
    return count;
  }

  private int postRecalculatedInterest(
      UUID debtId, List<InterestPeriod> periods, String reference) {

    int count = 0;
    LocalDate today = LocalDate.now();

    for (InterestPeriod period : periods) {
      if (period.getInterestAmount().compareTo(BigDecimal.ZERO) <= 0) {
        continue;
      }

      UUID transactionId = UUID.randomUUID();
      String desc =
          String.format(
              "Interest recalculated: %s to %s, principal=%s, days=%d",
              period.getPeriodStart(),
              period.getPeriodEnd(),
              period.getPrincipalBalance().toPlainString(),
              period.getDays());

      saveLedgerEntry(
          new LedgerEntryRequest(
              transactionId,
              debtId,
              kontoplan.interestReceivable().getCode(),
              kontoplan.interestReceivable().getName(),
              EntryType.DEBIT,
              period.getInterestAmount(),
              period.getPeriodStart(),
              today,
              reference,
              desc,
              null,
              EntryCategory.INTEREST_ACCRUAL));
      saveLedgerEntry(
          new LedgerEntryRequest(
              transactionId,
              debtId,
              kontoplan.interestRevenue().getCode(),
              kontoplan.interestRevenue().getName(),
              EntryType.CREDIT,
              period.getInterestAmount(),
              period.getPeriodStart(),
              today,
              reference,
              desc,
              null,
              EntryCategory.INTEREST_ACCRUAL));
      count += 2;
    }

    return count;
  }

  private BigDecimal calculateInterestTotal(List<LedgerEntry> entries) {
    return entries.stream()
        .filter(e -> e.getEntryType() == EntryType.DEBIT)
        .filter(e -> kontoplan.interestReceivable().getCode().equals(e.getAccountCode()))
        .map(LedgerEntry::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private void saveLedgerEntry(LedgerEntryRequest request) {
    LedgerEntry entry =
        LedgerEntry.builder()
            .transactionId(request.transactionId())
            .debtId(request.debtId())
            .accountCode(request.accountCode())
            .accountName(request.accountName())
            .entryType(request.entryType())
            .amount(request.amount())
            .effectiveDate(request.effectiveDate())
            .postingDate(request.postingDate())
            .reference(request.reference())
            .description(request.description())
            .reversalOfTransactionId(request.reversalOfTransactionId())
            .entryCategory(request.category())
            .build();

    ledgerEntryStore.saveSingle(entry);
  }

  private String correctionDescription(String reason) {
    return CORRECTION_PREFIX + reason;
  }

  private record LedgerEntryRequest(
      UUID transactionId,
      UUID debtId,
      String accountCode,
      String accountName,
      EntryType entryType,
      BigDecimal amount,
      LocalDate effectiveDate,
      LocalDate postingDate,
      String reference,
      String description,
      UUID reversalOfTransactionId,
      EntryCategory category) {}
}
