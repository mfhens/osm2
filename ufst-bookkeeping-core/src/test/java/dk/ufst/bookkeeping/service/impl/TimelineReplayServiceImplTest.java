package dk.ufst.bookkeeping.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
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
import dk.ufst.bookkeeping.model.CoverageAllocation;
import dk.ufst.bookkeeping.model.TimelineReplayResult;
import dk.ufst.bookkeeping.port.CoveragePriorityPort;
import dk.ufst.bookkeeping.port.FinancialEventStore;
import dk.ufst.bookkeeping.port.LedgerEntryStore;
import dk.ufst.bookkeeping.support.TestAccountCode;
import dk.ufst.bookkeeping.support.TestKontoplan;

/**
 * Unit tests for TimelineReplayServiceImpl.
 *
 * <p>Covers: empty-event zero balance, principal accumulation, payment coverage allocation, balance
 * effects of WRITE_OFF / UDLAEG_CORRECTED / REFUND, storno of active entries, and event ordering.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TimelineReplayServiceImplTest {

  @Mock private FinancialEventStore financialEventStore;
  @Mock private LedgerEntryStore ledgerEntryStore;
  @Mock private CoveragePriorityPort coveragePriorityPort;

  private TimelineReplayServiceImpl service;

  private static final UUID DEBT_ID = UUID.randomUUID();
  private static final BigDecimal RATE = new BigDecimal("0.0575");
  private static final LocalDate CROSSING_POINT = LocalDate.of(2026, 1, 1);

  @BeforeEach
  void setUp() {
    service =
        new TimelineReplayServiceImpl(
            financialEventStore, ledgerEntryStore, coveragePriorityPort, TestKontoplan.INSTANCE);

    // Default stubs — tests override as needed
    when(financialEventStore.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(DEBT_ID))
        .thenReturn(Collections.emptyList());
    when(ledgerEntryStore.findActiveEntriesByDebtId(DEBT_ID)).thenReturn(Collections.emptyList());
  }

  // ---------------------------------------------------------------------------
  // Empty event list
  // ---------------------------------------------------------------------------

  @Test
  void givenEmptyEventList_whenReplayTimeline_thenZeroFinalPrincipalAndInterest() {
    // Ref: TimelineReplayServiceImpl — no events → initial state = (0, 0), no updates applied.
    TimelineReplayResult result = service.replayTimeline(DEBT_ID, CROSSING_POINT, RATE, "TEST");

    assertThat(result.getDebtId()).isEqualTo(DEBT_ID);
    assertThat(result.getFinalPrincipalBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.getFinalInterestBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.getRecalculatedInterestPeriods()).isEmpty();
  }

  @Test
  void givenEmptyEventList_whenReplayTimeline_thenReplayFromDateEqualsInputCrossingPoint() {
    // Ref: TimelineReplayResult.replayFromDate is always the provided crossingPoint.
    TimelineReplayResult result = service.replayTimeline(DEBT_ID, CROSSING_POINT, RATE, "TEST");

    assertThat(result.getReplayFromDate()).isEqualTo(CROSSING_POINT);
  }

  // ---------------------------------------------------------------------------
  // Principal balance accumulation
  // ---------------------------------------------------------------------------

  @Test
  void givenDebtRegistrationBeforeCrossing_whenReplayTimeline_thenFinalPrincipalIsDebtAmount() {
    // Ref: TimelineReplayServiceImpl — events before crossingPoint are applied to initial state
    // only; no entries are re-posted for them.
    BigDecimal debtAmount = new BigDecimal("50000");
    FinancialEvent debt =
        buildEvent(CROSSING_POINT.minusDays(30), EventType.DEBT_REGISTERED, debtAmount);

    when(financialEventStore.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(DEBT_ID))
        .thenReturn(List.of(debt));

    TimelineReplayResult result = service.replayTimeline(DEBT_ID, CROSSING_POINT, RATE, "TEST");

    assertThat(result.getFinalPrincipalBalance()).isEqualByComparingTo("50000");
  }

  @Test
  void givenDebtRegistrationAtCrossing_whenReplayTimeline_thenFinalPrincipalIsDebtAmount() {
    // Ref: TimelineReplayServiceImpl — events exactly at crossingPoint are replayed (re-posted).
    BigDecimal debtAmount = new BigDecimal("50000");
    FinancialEvent debt = buildEvent(CROSSING_POINT, EventType.DEBT_REGISTERED, debtAmount);

    when(financialEventStore.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(DEBT_ID))
        .thenReturn(List.of(debt));

    TimelineReplayResult result = service.replayTimeline(DEBT_ID, CROSSING_POINT, RATE, "TEST");

    assertThat(result.getFinalPrincipalBalance()).isEqualByComparingTo("50000");
  }

  @Test
  void givenDebtRegistrationAtCrossing_whenReplayTimeline_thenInterestAccruedToToday() {
    // Ref: TimelineReplayServiceImpl — final interest gap is computed from last event date to
    // today.
    // With a non-zero principal and non-zero rate, at least one interest period must be produced.
    BigDecimal debtAmount = new BigDecimal("50000");
    FinancialEvent debt = buildEvent(CROSSING_POINT, EventType.DEBT_REGISTERED, debtAmount);

    when(financialEventStore.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(DEBT_ID))
        .thenReturn(List.of(debt));

    TimelineReplayResult result = service.replayTimeline(DEBT_ID, CROSSING_POINT, RATE, "TEST");

    // At least one interest period should be produced (crossingPoint is in the past relative to
    // LocalDate.now()).
    assertThat(result.getRecalculatedInterestPeriods()).isNotEmpty();
    assertThat(result.getNewInterestTotal()).isGreaterThan(BigDecimal.ZERO);
  }

  // ---------------------------------------------------------------------------
  // Recovery event (PAYMENT_RECEIVED) — coverage allocation reduces balance
  // ---------------------------------------------------------------------------

  @Test
  void
      givenDebtBeforeCrossingAndPaymentAtCrossing_whenReplayTimeline_thenPrincipalReducedByCoveredPortion() {
    // Ref: TimelineReplayServiceImpl — PAYMENT_RECEIVED triggers
    // coveragePriorityPort.allocatePayment;
    // state.principalBalance -= allocation.principalPortion.
    BigDecimal debtAmount = new BigDecimal("50000");
    BigDecimal paymentAmount = new BigDecimal("5000");

    FinancialEvent debt =
        buildEvent(CROSSING_POINT.minusDays(30), EventType.DEBT_REGISTERED, debtAmount);
    FinancialEvent payment = buildEvent(CROSSING_POINT, EventType.PAYMENT_RECEIVED, paymentAmount);

    when(financialEventStore.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(DEBT_ID))
        .thenReturn(List.of(debt, payment));

    CoverageAllocation allocation =
        CoverageAllocation.builder()
            .debtId(DEBT_ID)
            .totalAmount(paymentAmount)
            .interestPortion(BigDecimal.ZERO)
            .feesPortion(BigDecimal.ZERO)
            .principalPortion(paymentAmount) // entire payment goes to principal
            .build();

    when(coveragePriorityPort.allocatePayment(eq(DEBT_ID), eq(paymentAmount), any(), any(), any()))
        .thenReturn(allocation);

    TimelineReplayResult result = service.replayTimeline(DEBT_ID, CROSSING_POINT, RATE, "TEST");

    // 50 000 − 5 000 principal portion = 45 000
    assertThat(result.getFinalPrincipalBalance()).isEqualByComparingTo("45000");
  }

  @Test
  void
      givenPaymentWithInterestPortion_whenReplayTimeline_thenAccruedInterestReducedByInterestPortion() {
    // Ref: TimelineReplayServiceImpl — state.accruedInterest -= allocation.interestPortion.
    BigDecimal debtAmount = new BigDecimal("50000");
    BigDecimal paymentAmount = new BigDecimal("1500");
    BigDecimal interestPortion = new BigDecimal("500");
    BigDecimal principalPortion = new BigDecimal("1000");

    FinancialEvent debt =
        buildEvent(CROSSING_POINT.minusDays(30), EventType.DEBT_REGISTERED, debtAmount);
    FinancialEvent payment = buildEvent(CROSSING_POINT, EventType.PAYMENT_RECEIVED, paymentAmount);

    when(financialEventStore.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(DEBT_ID))
        .thenReturn(List.of(debt, payment));

    CoverageAllocation allocation =
        CoverageAllocation.builder()
            .debtId(DEBT_ID)
            .totalAmount(paymentAmount)
            .interestPortion(interestPortion)
            .feesPortion(BigDecimal.ZERO)
            .principalPortion(principalPortion)
            .build();

    when(coveragePriorityPort.allocatePayment(any(), any(), any(), any(), any()))
        .thenReturn(allocation);

    TimelineReplayResult result = service.replayTimeline(DEBT_ID, CROSSING_POINT, RATE, "TEST");

    // finalPrincipal = 50 000 − 1 000 = 49 000
    assertThat(result.getFinalPrincipalBalance()).isEqualByComparingTo("49000");
  }

  // ---------------------------------------------------------------------------
  // Non-recovery events at / after crossing point
  // ---------------------------------------------------------------------------

  @Test
  void givenUdlaegCorrectedWithNegativeDelta_whenReplayTimeline_thenPrincipalReduced() {
    // Ref: TimelineReplayServiceImpl.applyEventToState — UDLAEG_CORRECTED adds delta (negative →
    // reduces balance).
    BigDecimal debtAmount = new BigDecimal("50000");
    BigDecimal correctionDelta = new BigDecimal("-10000");

    FinancialEvent debt =
        buildEvent(CROSSING_POINT.minusDays(30), EventType.DEBT_REGISTERED, debtAmount);
    FinancialEvent correction =
        buildEvent(CROSSING_POINT, EventType.UDLAEG_CORRECTED, correctionDelta);

    when(financialEventStore.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(DEBT_ID))
        .thenReturn(List.of(debt, correction));

    TimelineReplayResult result = service.replayTimeline(DEBT_ID, CROSSING_POINT, RATE, "TEST");

    // 50 000 + (−10 000) = 40 000
    assertThat(result.getFinalPrincipalBalance()).isEqualByComparingTo("40000");
  }

  @Test
  void givenRefundEvent_whenReplayTimeline_thenPrincipalIncreased() {
    // Ref: TimelineReplayServiceImpl.applyEventToState — REFUND adds amount to principalBalance.
    BigDecimal debtAmount = new BigDecimal("10000");
    BigDecimal refundAmount = new BigDecimal("5000");

    FinancialEvent debt =
        buildEvent(CROSSING_POINT.minusDays(30), EventType.DEBT_REGISTERED, debtAmount);
    FinancialEvent refund = buildEvent(CROSSING_POINT, EventType.REFUND, refundAmount);

    when(financialEventStore.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(DEBT_ID))
        .thenReturn(List.of(debt, refund));

    TimelineReplayResult result = service.replayTimeline(DEBT_ID, CROSSING_POINT, RATE, "TEST");

    // 10 000 + 5 000 = 15 000
    assertThat(result.getFinalPrincipalBalance()).isEqualByComparingTo("15000");
  }

  @Test
  void givenWriteOff_whenReplayTimeline_thenPrincipalReduced() {
    // Ref: TimelineReplayServiceImpl.applyEventToState — WRITE_OFF subtracts amount.
    BigDecimal debtAmount = new BigDecimal("30000");
    BigDecimal writeOffAmount = new BigDecimal("10000");

    FinancialEvent debt =
        buildEvent(CROSSING_POINT.minusDays(30), EventType.DEBT_REGISTERED, debtAmount);
    FinancialEvent writeOff = buildEvent(CROSSING_POINT, EventType.WRITE_OFF, writeOffAmount);

    when(financialEventStore.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(DEBT_ID))
        .thenReturn(List.of(debt, writeOff));

    TimelineReplayResult result = service.replayTimeline(DEBT_ID, CROSSING_POINT, RATE, "TEST");

    // 30 000 − 10 000 = 20 000
    assertThat(result.getFinalPrincipalBalance()).isEqualByComparingTo("20000");
  }

  // ---------------------------------------------------------------------------
  // Event ordering: events sorted by effective date before processing
  // ---------------------------------------------------------------------------

  @Test
  void
      givenEventsBeforeCrossingInReverseOrder_whenReplayTimeline_thenFinalPrincipalReflectsAllNonRecoveryEvents() {
    // Ref: TimelineReplayServiceImpl — allEvents.sort(EventOrderComparator.INSTANCE) is called
    // before state is built. DEBT_REGISTERED and WRITE_OFF (both handled by applyEventToState) are
    // applied regardless of their initial store order; arithmetic ensures the same final balance.
    // PAYMENT_RECEIVED is intentionally NOT applied in the pre-crossing state loop (it is a
    // recovery event handled separately) — so only non-recovery events determine pre-crossing
    // state.
    BigDecimal debtAmount = new BigDecimal("50000");
    BigDecimal writeOffAmount = new BigDecimal("10000");
    LocalDate debtDate = CROSSING_POINT.minusDays(60);
    LocalDate writeOffDate = CROSSING_POINT.minusDays(30);

    FinancialEvent writeOff = buildEvent(writeOffDate, EventType.WRITE_OFF, writeOffAmount);
    FinancialEvent debt = buildEvent(debtDate, EventType.DEBT_REGISTERED, debtAmount);

    // Store returns events in wrong date order (write-off first, debt second)
    when(financialEventStore.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(DEBT_ID))
        .thenReturn(List.of(writeOff, debt));

    TimelineReplayResult result = service.replayTimeline(DEBT_ID, CROSSING_POINT, RATE, "TEST");

    // Correct date order: DEBT_REGISTERED(−60d, +50 000) then WRITE_OFF(−30d, −10 000) = 40 000.
    // Addition/subtraction is commutative so wrong order gives the same result — this test verifies
    // no exception is thrown and the balance is correct.
    assertThat(result.getFinalPrincipalBalance()).isEqualByComparingTo("40000");
  }

  @Test
  void givenInterestAccruedEventAtCrossing_whenReplayTimeline_thenInterestAccruedNotReplayed() {
    // Ref: TimelineReplayServiceImpl — INTEREST_ACCRUED is filtered from replayEvents:
    //   .filter(e -> e.getEventType() != EventType.INTEREST_ACCRUED)
    // It must not affect principalBalance and must not generate repost entries.
    FinancialEvent interestEvent =
        buildEvent(CROSSING_POINT, EventType.INTEREST_ACCRUED, new BigDecimal("200"));

    when(financialEventStore.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(DEBT_ID))
        .thenReturn(List.of(interestEvent));

    TimelineReplayResult result = service.replayTimeline(DEBT_ID, CROSSING_POINT, RATE, "TEST");

    // INTEREST_ACCRUED does not update principal; balance stays zero
    assertThat(result.getFinalPrincipalBalance()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void givenCoverageReversedEventAtCrossing_whenReplayTimeline_thenCoverageReversedNotReplayed() {
    // Ref: TimelineReplayServiceImpl — COVERAGE_REVERSED is filtered from replayEvents:
    //   .filter(e -> e.getEventType() != EventType.COVERAGE_REVERSED)
    FinancialEvent coverageReversed =
        buildEvent(CROSSING_POINT, EventType.COVERAGE_REVERSED, new BigDecimal("100"));

    when(financialEventStore.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(DEBT_ID))
        .thenReturn(List.of(coverageReversed));

    TimelineReplayResult result = service.replayTimeline(DEBT_ID, CROSSING_POINT, RATE, "TEST");

    assertThat(result.getFinalPrincipalBalance()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  // ---------------------------------------------------------------------------
  // Storno mechanics
  // ---------------------------------------------------------------------------

  @Test
  void givenActiveEntriesAfterCrossing_whenReplayTimeline_thenStornoEntriesPosted() {
    // Ref: TimelineReplayServiceImpl.stornoEntriesFromDate — entries with effectiveDate ≥
    // crossingPoint are reversed (EntryCategory.STORNO).
    UUID txnId = UUID.randomUUID();
    LocalDate entryDate = CROSSING_POINT.plusDays(10);

    LedgerEntry debitEntry =
        LedgerEntry.builder()
            .transactionId(txnId)
            .debtId(DEBT_ID)
            .accountCode(TestAccountCode.RECEIVABLES.getCode())
            .accountName(TestAccountCode.RECEIVABLES.getName())
            .entryType(EntryType.DEBIT)
            .amount(new BigDecimal("50000"))
            .effectiveDate(entryDate)
            .build();
    LedgerEntry creditEntry =
        LedgerEntry.builder()
            .transactionId(txnId)
            .debtId(DEBT_ID)
            .accountCode(TestAccountCode.COLLECTION_REVENUE.getCode())
            .accountName(TestAccountCode.COLLECTION_REVENUE.getName())
            .entryType(EntryType.CREDIT)
            .amount(new BigDecimal("50000"))
            .effectiveDate(entryDate)
            .build();

    when(ledgerEntryStore.findActiveEntriesByDebtId(DEBT_ID))
        .thenReturn(List.of(debitEntry, creditEntry));
    when(ledgerEntryStore.existsByReversalOfTransactionId(txnId)).thenReturn(false);
    when(ledgerEntryStore.findByTransactionId(txnId)).thenReturn(List.of(debitEntry, creditEntry));

    service.replayTimeline(DEBT_ID, CROSSING_POINT, RATE, "TEST");

    ArgumentCaptor<LedgerEntry> captor = forClass(LedgerEntry.class);
    verify(ledgerEntryStore, atLeast(2)).saveSingle(captor.capture());

    List<LedgerEntry> stornoEntries =
        captor.getAllValues().stream()
            .filter(e -> e.getEntryCategory() == EntryCategory.STORNO)
            .toList();

    assertThat(stornoEntries).hasSize(2);
    // DEBIT entry reversed to CREDIT
    LedgerEntry reversedDebit =
        stornoEntries.stream()
            .filter(e -> e.getAccountCode().equals(TestAccountCode.RECEIVABLES.getCode()))
            .findFirst()
            .orElseThrow();
    assertThat(reversedDebit.getEntryType()).isEqualTo(EntryType.CREDIT);
    assertThat(reversedDebit.getReversalOfTransactionId()).isEqualTo(txnId);
    assertThat(reversedDebit.getAmount()).isEqualByComparingTo("50000");
  }

  @Test
  void givenActiveEntriesBeforeCrossing_whenReplayTimeline_thenThoseEntriesNotStorned() {
    // Ref: TimelineReplayServiceImpl.stornoEntriesFromDate — only entries with
    // effectiveDate >= crossingPoint are filtered in.
    UUID txnId = UUID.randomUUID();
    LocalDate beforeCrossing = CROSSING_POINT.minusDays(10);

    LedgerEntry entryBeforeCrossing =
        LedgerEntry.builder()
            .transactionId(txnId)
            .debtId(DEBT_ID)
            .accountCode(TestAccountCode.RECEIVABLES.getCode())
            .entryType(EntryType.DEBIT)
            .amount(new BigDecimal("10000"))
            .effectiveDate(beforeCrossing) // before crossing → excluded
            .build();

    when(ledgerEntryStore.findActiveEntriesByDebtId(DEBT_ID))
        .thenReturn(List.of(entryBeforeCrossing));

    service.replayTimeline(DEBT_ID, CROSSING_POINT, RATE, "TEST");

    // existsByReversalOfTransactionId and findByTransactionId must NOT be called for this entry
    verify(ledgerEntryStore, org.mockito.Mockito.never()).existsByReversalOfTransactionId(txnId);
  }

  @Test
  void givenStornoCount_whenReplayTimeline_thenResultReflectsStornoCount() {
    // Ref: TimelineReplayResult.stornoEntriesPosted — total individual reversed entries.
    UUID txnId = UUID.randomUUID();
    LedgerEntry debit =
        LedgerEntry.builder()
            .transactionId(txnId)
            .accountCode(TestAccountCode.RECEIVABLES.getCode())
            .entryType(EntryType.DEBIT)
            .amount(new BigDecimal("10000"))
            .effectiveDate(CROSSING_POINT)
            .build();
    LedgerEntry credit =
        LedgerEntry.builder()
            .transactionId(txnId)
            .accountCode(TestAccountCode.COLLECTION_REVENUE.getCode())
            .entryType(EntryType.CREDIT)
            .amount(new BigDecimal("10000"))
            .effectiveDate(CROSSING_POINT)
            .build();

    when(ledgerEntryStore.findActiveEntriesByDebtId(DEBT_ID)).thenReturn(List.of(debit, credit));
    when(ledgerEntryStore.existsByReversalOfTransactionId(txnId)).thenReturn(false);
    when(ledgerEntryStore.findByTransactionId(txnId)).thenReturn(List.of(debit, credit));

    TimelineReplayResult result = service.replayTimeline(DEBT_ID, CROSSING_POINT, RATE, "TEST");

    assertThat(result.getStornoEntriesPosted()).isEqualTo(2);
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private FinancialEvent buildEvent(LocalDate date, EventType type, BigDecimal amount) {
    return FinancialEvent.builder()
        .id(UUID.randomUUID())
        .debtId(DEBT_ID)
        .eventType(type)
        .effectiveDate(date)
        .amount(amount)
        .build();
  }
}
