package dk.ufst.bookkeeping.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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
import dk.ufst.bookkeeping.support.TestAccountCode;
import dk.ufst.bookkeeping.support.TestKontoplan;

/**
 * Unit tests for RetroactiveCorrectionServiceImpl.
 *
 * <p>Covers: storno mechanics, principal correction ledger entries, interest delta calculation,
 * already-reversed guard, and no-op when originalAmount == correctedAmount.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RetroactiveCorrectionServiceImplTest {

  @Mock private LedgerEntryStore ledgerEntryStore;
  @Mock private FinancialEventStore financialEventStore;
  @Mock private InterestAccrualService interestAccrualService;

  private RetroactiveCorrectionServiceImpl service;

  private static final UUID DEBT_ID = UUID.randomUUID();
  private static final LocalDate EFFECTIVE_DATE = LocalDate.of(2025, 12, 1);
  private static final BigDecimal ANNUAL_RATE = new BigDecimal("0.10");

  @BeforeEach
  void setUp() {
    service =
        new RetroactiveCorrectionServiceImpl(
            ledgerEntryStore, financialEventStore, interestAccrualService, TestKontoplan.INSTANCE);

    // Default stubs — individual tests override as needed.
    // AIDEV-NOTE: Using thenReturn (not thenAnswer) avoids NPE when Mockito internally calls
    // save(null) during a subsequent when(...) recording phase in test methods.
    FinancialEvent defaultSaved = FinancialEvent.builder().id(UUID.randomUUID()).build();
    when(financialEventStore.save(any())).thenReturn(defaultSaved);
    when(ledgerEntryStore.findInterestAccrualsAfterDate(any(), any()))
        .thenReturn(Collections.emptyList());
    when(interestAccrualService.calculatePeriodicInterest(any(), any(), any(), any()))
        .thenReturn(Collections.emptyList());
  }

  // ---------------------------------------------------------------------------
  // Correction event recording
  // ---------------------------------------------------------------------------

  @Test
  void givenCorrection_whenApplyRetroactiveCorrection_thenCorrectionEventSavedToStore() {
    // Ref: RetroactiveCorrectionServiceImpl.recordCorrectionEvent — always saves UDLAEG_CORRECTED
    UUID correctionEventId = UUID.randomUUID();
    FinancialEvent savedEvent = FinancialEvent.builder().id(correctionEventId).build();
    when(financialEventStore.save(any())).thenReturn(savedEvent);

    CorrectionResult result =
        service.applyRetroactiveCorrection(
            DEBT_ID, EFFECTIVE_DATE, new BigDecimal("50000"), new BigDecimal("30000"),
            ANNUAL_RATE, "RET-001", "Udlaeg nedsat");

    ArgumentCaptor<FinancialEvent> captor = forClass(FinancialEvent.class);
    verify(financialEventStore).save(captor.capture());
    assertThat(captor.getValue().getEventType()).isEqualTo(EventType.UDLAEG_CORRECTED);
    assertThat(result.getCorrectionEventId()).isEqualTo(correctionEventId);
  }

  // ---------------------------------------------------------------------------
  // Principal delta direction
  // ---------------------------------------------------------------------------

  @Test
  void givenPrincipalDecrease_whenApplyRetroactiveCorrection_thenNegativePrincipalDelta() {
    // Ref: RetroactiveCorrectionServiceImpl — principalDelta = correctedAmount − originalAmount
    CorrectionResult result =
        service.applyRetroactiveCorrection(
            DEBT_ID, EFFECTIVE_DATE, new BigDecimal("50000"), new BigDecimal("30000"),
            ANNUAL_RATE, "RET-002", "Decreased");

    assertThat(result.getPrincipalDelta()).isEqualByComparingTo("-20000");
  }

  @Test
  void givenPrincipalIncrease_whenApplyRetroactiveCorrection_thenPositivePrincipalDelta() {
    // Ref: RetroactiveCorrectionServiceImpl — positive delta when corrected > original
    CorrectionResult result =
        service.applyRetroactiveCorrection(
            DEBT_ID, EFFECTIVE_DATE, new BigDecimal("30000"), new BigDecimal("50000"),
            ANNUAL_RATE, "RET-003", "Increased");

    assertThat(result.getPrincipalDelta()).isEqualByComparingTo("20000");
  }

  // ---------------------------------------------------------------------------
  // Principal correction ledger entries
  // ---------------------------------------------------------------------------

  @Test
  void givenPrincipalDecrease_whenApplyRetroactiveCorrection_thenCorrectionEntriesPostedWithCorrectAccounts() {
    // Ref: RetroactiveCorrectionServiceImpl.postPrincipalCorrection (delta < 0):
    // DEBIT collectionRevenue, CREDIT receivables with absDelta.
    service.applyRetroactiveCorrection(
        DEBT_ID, EFFECTIVE_DATE, new BigDecimal("50000"), new BigDecimal("30000"),
        ANNUAL_RATE, "RET-004", "Nedsat");

    ArgumentCaptor<LedgerEntry> captor = forClass(LedgerEntry.class);
    verify(ledgerEntryStore, org.mockito.Mockito.atLeast(2)).saveSingle(captor.capture());

    List<LedgerEntry> correctionEntries =
        captor.getAllValues().stream()
            .filter(e -> e.getEntryCategory() == EntryCategory.CORRECTION)
            .toList();

    assertThat(correctionEntries).hasSize(2);

    LedgerEntry debitEntry =
        correctionEntries.stream()
            .filter(e -> e.getEntryType() == EntryType.DEBIT)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Expected a DEBIT correction entry"));
    LedgerEntry creditEntry =
        correctionEntries.stream()
            .filter(e -> e.getEntryType() == EntryType.CREDIT)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Expected a CREDIT correction entry"));

    assertThat(debitEntry.getAccountCode())
        .isEqualTo(TestAccountCode.COLLECTION_REVENUE.getCode());
    assertThat(creditEntry.getAccountCode()).isEqualTo(TestAccountCode.RECEIVABLES.getCode());

    // Absolute delta
    assertThat(debitEntry.getAmount()).isEqualByComparingTo("20000");
    assertThat(creditEntry.getAmount()).isEqualByComparingTo("20000");
  }

  @Test
  void givenPrincipalIncrease_whenApplyRetroactiveCorrection_thenCorrectionEntriesPostedWithCorrectAccounts() {
    // Ref: RetroactiveCorrectionServiceImpl.postPrincipalCorrection (delta > 0):
    // DEBIT receivables, CREDIT collectionRevenue with delta.
    service.applyRetroactiveCorrection(
        DEBT_ID, EFFECTIVE_DATE, new BigDecimal("30000"), new BigDecimal("50000"),
        ANNUAL_RATE, "RET-005", "Ophojet");

    ArgumentCaptor<LedgerEntry> captor = forClass(LedgerEntry.class);
    verify(ledgerEntryStore, org.mockito.Mockito.atLeast(2)).saveSingle(captor.capture());

    List<LedgerEntry> correctionEntries =
        captor.getAllValues().stream()
            .filter(e -> e.getEntryCategory() == EntryCategory.CORRECTION)
            .toList();
    assertThat(correctionEntries).hasSize(2);

    LedgerEntry debitEntry =
        correctionEntries.stream()
            .filter(e -> e.getEntryType() == EntryType.DEBIT)
            .findFirst()
            .orElseThrow();
    assertThat(debitEntry.getAccountCode()).isEqualTo(TestAccountCode.RECEIVABLES.getCode());
    assertThat(debitEntry.getAmount()).isEqualByComparingTo("20000");
  }

  @Test
  void givenSameOriginalAndCorrectedAmount_whenApplyRetroactiveCorrection_thenNoPrincipalCorrectionEntries() {
    // Ref: RetroactiveCorrectionServiceImpl.postPrincipalCorrection — delta == 0 → neither branch
    // fires → no saveSingle calls for principal correction. The storno and interest paths still run
    // (controlled here by returning empty lists).
    service.applyRetroactiveCorrection(
        DEBT_ID, EFFECTIVE_DATE, new BigDecimal("50000"), new BigDecimal("50000"),
        ANNUAL_RATE, "RET-006", "No change");

    ArgumentCaptor<LedgerEntry> captor = forClass(LedgerEntry.class);
    // saveSingle must not be called for CORRECTION category
    verify(ledgerEntryStore, never())
        .saveSingle(
            org.mockito.ArgumentMatchers.argThat(
                e -> e.getEntryCategory() == EntryCategory.CORRECTION));
  }

  // ---------------------------------------------------------------------------
  // Storno mechanics
  // ---------------------------------------------------------------------------

  @Test
  void givenInterestEntries_whenApplyRetroactiveCorrection_thenStornoEntriesPostedWithReversedTypes() {
    // Ref: RetroactiveCorrectionServiceImpl.stornoInterestEntries — DEBIT ↔ CREDIT swap.
    UUID txnId = UUID.randomUUID();
    LedgerEntry debitInterest = buildInterestEntry(txnId, EntryType.DEBIT, "500");
    LedgerEntry creditInterest = buildInterestEntry(txnId, EntryType.CREDIT, "500");

    when(ledgerEntryStore.findInterestAccrualsAfterDate(DEBT_ID, EFFECTIVE_DATE))
        .thenReturn(List.of(debitInterest, creditInterest));
    when(ledgerEntryStore.existsByReversalOfTransactionId(txnId)).thenReturn(false);
    when(ledgerEntryStore.findByTransactionId(txnId))
        .thenReturn(List.of(debitInterest, creditInterest));

    service.applyRetroactiveCorrection(
        DEBT_ID, EFFECTIVE_DATE, new BigDecimal("50000"), new BigDecimal("30000"),
        ANNUAL_RATE, "RET-007", "Storno test");

    ArgumentCaptor<LedgerEntry> captor = forClass(LedgerEntry.class);
    verify(ledgerEntryStore, org.mockito.Mockito.atLeast(1)).saveSingle(captor.capture());

    List<LedgerEntry> stornoEntries =
        captor.getAllValues().stream()
            .filter(e -> e.getEntryCategory() == EntryCategory.STORNO)
            .toList();

    assertThat(stornoEntries).hasSize(2);

    // The DEBIT entry should be reversed to CREDIT
    LedgerEntry reversedDebit =
        stornoEntries.stream()
            .filter(
                e ->
                    e.getAccountCode().equals(TestAccountCode.INTEREST_RECEIVABLE.getCode())
                        && e.getEntryType() == EntryType.CREDIT)
            .findFirst()
            .orElseThrow(() -> new AssertionError("Expected reversed (CREDIT) entry for INTEREST_RECEIVABLE"));
    assertThat(reversedDebit.getReversalOfTransactionId()).isEqualTo(txnId);
    assertThat(reversedDebit.getAmount()).isEqualByComparingTo("500");
  }

  @Test
  void givenAlreadyReversedTransaction_whenApplyRetroactiveCorrection_thenStornoSkipped() {
    // Ref: RetroactiveCorrectionServiceImpl.stornoInterestEntries — existsByReversalOfTransactionId
    // == true → skip that transaction.
    UUID txnId = UUID.randomUUID();
    LedgerEntry entry = buildInterestEntry(txnId, EntryType.DEBIT, "500");

    when(ledgerEntryStore.findInterestAccrualsAfterDate(DEBT_ID, EFFECTIVE_DATE))
        .thenReturn(List.of(entry));
    when(ledgerEntryStore.existsByReversalOfTransactionId(txnId)).thenReturn(true);

    service.applyRetroactiveCorrection(
        DEBT_ID, EFFECTIVE_DATE, new BigDecimal("50000"), new BigDecimal("30000"),
        ANNUAL_RATE, "RET-008", "Already reversed");

    // findByTransactionId must never be called for the already-reversed transaction
    verify(ledgerEntryStore, never()).findByTransactionId(txnId);
    // saveSingle must not be called for STORNO
    verify(ledgerEntryStore, never())
        .saveSingle(
            org.mockito.ArgumentMatchers.argThat(
                e -> e.getEntryCategory() == EntryCategory.STORNO));
  }

  @Test
  void givenStornoCount_whenApplyRetroactiveCorrection_thenResultReflectsStornoCount() {
    // Ref: CorrectionResult.stornoEntriesPosted — counts individual reversed entries.
    UUID txnId = UUID.randomUUID();
    LedgerEntry debitInterest = buildInterestEntry(txnId, EntryType.DEBIT, "500");
    LedgerEntry creditInterest = buildInterestEntry(txnId, EntryType.CREDIT, "500");

    when(ledgerEntryStore.findInterestAccrualsAfterDate(DEBT_ID, EFFECTIVE_DATE))
        .thenReturn(List.of(debitInterest, creditInterest));
    when(ledgerEntryStore.existsByReversalOfTransactionId(txnId)).thenReturn(false);
    when(ledgerEntryStore.findByTransactionId(txnId))
        .thenReturn(List.of(debitInterest, creditInterest));

    CorrectionResult result =
        service.applyRetroactiveCorrection(
            DEBT_ID, EFFECTIVE_DATE, new BigDecimal("50000"), new BigDecimal("30000"),
            ANNUAL_RATE, "RET-009", "Count check");

    assertThat(result.getStornoEntriesPosted()).isEqualTo(2);
  }

  // ---------------------------------------------------------------------------
  // Interest recalculation and delta
  // ---------------------------------------------------------------------------

  @Test
  void givenNewInterestPeriods_whenApplyRetroactiveCorrection_thenNewEntriesPostedPerPeriod() {
    // Ref: RetroactiveCorrectionServiceImpl.postRecalculatedInterest — 2 entries per period.
    InterestPeriod period1 = buildPeriod(LocalDate.of(2025, 12, 1), LocalDate.of(2026, 1, 1), "300.00");
    InterestPeriod period2 = buildPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1), "500.00");

    when(interestAccrualService.calculatePeriodicInterest(eq(DEBT_ID), any(), any(), eq(ANNUAL_RATE)))
        .thenReturn(List.of(period1, period2));

    CorrectionResult result =
        service.applyRetroactiveCorrection(
            DEBT_ID, EFFECTIVE_DATE, new BigDecimal("50000"), new BigDecimal("30000"),
            ANNUAL_RATE, "RET-010", "New interest");

    // 2 periods × 2 entries each = 4 new interest entries
    assertThat(result.getNewInterestEntriesPosted()).isEqualTo(4);
  }

  @Test
  void givenOldAndNewInterestTotals_whenApplyRetroactiveCorrection_thenInterestDeltaIsCorrect() {
    // Ref: CorrectionResult.interestDelta = newInterestTotal − oldInterestTotal.
    // Old: DEBIT on interestReceivable (500) → oldInterestTotal = 500.
    // New: two periods summing to 800 → newInterestTotal = 800.
    // Expected interestDelta = 300.
    UUID txnId = UUID.randomUUID();
    LedgerEntry oldDebit = buildInterestEntry(txnId, EntryType.DEBIT, "500");
    LedgerEntry oldCredit = buildInterestEntry(txnId, EntryType.CREDIT, "500");

    when(ledgerEntryStore.findInterestAccrualsAfterDate(DEBT_ID, EFFECTIVE_DATE))
        .thenReturn(List.of(oldDebit, oldCredit));
    when(ledgerEntryStore.existsByReversalOfTransactionId(txnId)).thenReturn(false);
    when(ledgerEntryStore.findByTransactionId(txnId)).thenReturn(List.of(oldDebit, oldCredit));

    InterestPeriod p1 = buildPeriod(LocalDate.of(2025, 12, 1), LocalDate.of(2026, 1, 1), "300.00");
    InterestPeriod p2 = buildPeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 1), "500.00");
    when(interestAccrualService.calculatePeriodicInterest(any(), any(), any(), any()))
        .thenReturn(List.of(p1, p2));

    CorrectionResult result =
        service.applyRetroactiveCorrection(
            DEBT_ID, EFFECTIVE_DATE, new BigDecimal("50000"), new BigDecimal("30000"),
            ANNUAL_RATE, "RET-011", "Delta check");

    assertThat(result.getOldInterestTotal()).isEqualByComparingTo("500");
    assertThat(result.getNewInterestTotal()).isEqualByComparingTo("800");
    assertThat(result.getInterestDelta()).isEqualByComparingTo("300");
  }

  @Test
  void givenNewInterestGreaterThanOld_whenApplyRetroactiveCorrection_thenPositiveInterestDelta() {
    // Ref: CorrectionResult.interestDelta sign — positive when new > old.
    InterestPeriod p = buildPeriod(LocalDate.of(2025, 12, 1), LocalDate.of(2026, 3, 1), "1200.00");
    when(interestAccrualService.calculatePeriodicInterest(any(), any(), any(), any()))
        .thenReturn(List.of(p));

    CorrectionResult result =
        service.applyRetroactiveCorrection(
            DEBT_ID, EFFECTIVE_DATE, new BigDecimal("50000"), new BigDecimal("80000"),
            ANNUAL_RATE, "RET-012", "Positive delta");

    assertThat(result.getInterestDelta()).isGreaterThan(BigDecimal.ZERO);
  }

  @Test
  void givenNewInterestLessThanOld_whenApplyRetroactiveCorrection_thenNegativeInterestDelta() {
    // Ref: CorrectionResult.interestDelta sign — negative when new < old.
    UUID txnId = UUID.randomUUID();
    LedgerEntry oldDebit = buildInterestEntry(txnId, EntryType.DEBIT, "2000");
    LedgerEntry oldCredit = buildInterestEntry(txnId, EntryType.CREDIT, "2000");

    when(ledgerEntryStore.findInterestAccrualsAfterDate(DEBT_ID, EFFECTIVE_DATE))
        .thenReturn(List.of(oldDebit, oldCredit));
    when(ledgerEntryStore.existsByReversalOfTransactionId(txnId)).thenReturn(false);
    when(ledgerEntryStore.findByTransactionId(txnId)).thenReturn(List.of(oldDebit, oldCredit));

    InterestPeriod p = buildPeriod(LocalDate.of(2025, 12, 1), LocalDate.of(2026, 3, 1), "500.00");
    when(interestAccrualService.calculatePeriodicInterest(any(), any(), any(), any()))
        .thenReturn(List.of(p));

    CorrectionResult result =
        service.applyRetroactiveCorrection(
            DEBT_ID, EFFECTIVE_DATE, new BigDecimal("50000"), new BigDecimal("30000"),
            ANNUAL_RATE, "RET-013", "Negative delta");

    assertThat(result.getInterestDelta()).isLessThan(BigDecimal.ZERO);
    assertThat(result.getInterestDelta()).isEqualByComparingTo("-1500");
  }

  @Test
  void givenRecalculation_whenApplyRetroactiveCorrection_thenRecalculatedPeriodsInResult() {
    // Ref: CorrectionResult.recalculatedPeriods — contains the newly computed periods.
    InterestPeriod p = buildPeriod(LocalDate.of(2025, 12, 1), LocalDate.of(2026, 3, 1), "750.00");
    when(interestAccrualService.calculatePeriodicInterest(any(), any(), any(), any()))
        .thenReturn(List.of(p));

    CorrectionResult result =
        service.applyRetroactiveCorrection(
            DEBT_ID, EFFECTIVE_DATE, new BigDecimal("50000"), new BigDecimal("30000"),
            ANNUAL_RATE, "RET-014", "Periods check");

    assertThat(result.getRecalculatedPeriods()).hasSize(1);
    assertThat(result.getRecalculatedPeriods().get(0).getInterestAmount())
        .isEqualByComparingTo("750.00");
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private LedgerEntry buildInterestEntry(UUID txnId, EntryType type, String amount) {
    return LedgerEntry.builder()
        .transactionId(txnId)
        .debtId(DEBT_ID)
        .accountCode(TestAccountCode.INTEREST_RECEIVABLE.getCode())
        .accountName(TestAccountCode.INTEREST_RECEIVABLE.getName())
        .entryType(type)
        .amount(new BigDecimal(amount))
        .effectiveDate(EFFECTIVE_DATE)
        .entryCategory(EntryCategory.INTEREST_ACCRUAL)
        .build();
  }

  private InterestPeriod buildPeriod(LocalDate start, LocalDate end, String interestAmount) {
    long days = java.time.temporal.ChronoUnit.DAYS.between(start, end);
    return InterestPeriod.builder()
        .periodStart(start)
        .periodEnd(end)
        .principalBalance(new BigDecimal("30000"))
        .annualRate(ANNUAL_RATE)
        .days(days)
        .interestAmount(new BigDecimal(interestAmount))
        .build();
  }
}
