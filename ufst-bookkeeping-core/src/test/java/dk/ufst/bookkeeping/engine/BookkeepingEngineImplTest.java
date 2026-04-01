package dk.ufst.bookkeeping.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dk.ufst.bookkeeping.domain.EntryCategory;
import dk.ufst.bookkeeping.domain.EntryType;
import dk.ufst.bookkeeping.domain.EventType;
import dk.ufst.bookkeeping.domain.FinancialEvent;
import dk.ufst.bookkeeping.domain.LedgerEntry;
import dk.ufst.bookkeeping.port.FinancialEventStore;
import dk.ufst.bookkeeping.port.LedgerEntryStore;
import dk.ufst.bookkeeping.support.TestAccountCode;

/**
 * Unit tests for BookkeepingEngineImpl.
 *
 * <p>Verifies the double-entry structural invariants: exactly one debit + one credit per call,
 * shared transactionId, equal amounts, correct effective/posting dates, account code pass-through,
 * and event recording.
 *
 * <p>AIDEV-NOTE: The balance guard inside postDoubleEntry compares debitEntry.getAmount() to
 * creditEntry.getAmount(). Since both entries are always constructed from the same {@code amount}
 * parameter the guard is currently unreachable dead code. The tests below verify the positive-path
 * invariant — both entries always carry equal amounts — rather than attempting to reach the dead
 * branch.
 */
@ExtendWith(MockitoExtension.class)
class BookkeepingEngineImplTest {

  @Mock private LedgerEntryStore ledgerEntryStore;
  @Mock private FinancialEventStore financialEventStore;

  private BookkeepingEngineImpl engine;

  private static final UUID DEBT_ID = UUID.randomUUID();
  private static final BigDecimal AMOUNT = new BigDecimal("50000.00");
  private static final LocalDate EFFECTIVE_DATE = LocalDate.of(2025, 10, 1);

  @BeforeEach
  void setUp() {
    engine = new BookkeepingEngineImpl(ledgerEntryStore, financialEventStore);
  }

  // ---------------------------------------------------------------------------
  // postDoubleEntry — structural invariants
  // ---------------------------------------------------------------------------

  @Test
  void givenValidAmount_whenPostDoubleEntry_thenSaveDoubleEntryCalledExactlyOnce() {
    // Ref: BookkeepingEngineImpl.postDoubleEntry — single saveDoubleEntry call
    engine.postDoubleEntry(
        DEBT_ID,
        TestAccountCode.RECEIVABLES,
        TestAccountCode.COLLECTION_REVENUE,
        AMOUNT,
        EFFECTIVE_DATE,
        "REF-001",
        "Debt registration",
        EntryCategory.DEBT_REGISTRATION);

    verify(ledgerEntryStore)
        .saveDoubleEntry(forClass(LedgerEntry.class).capture(), forClass(LedgerEntry.class).capture());
  }

  @Test
  void givenValidAmount_whenPostDoubleEntry_thenBothEntriesShareSameTransactionId() {
    // Ref: BookkeepingEngineImpl — transactionId = UUID.randomUUID() shared by both entries
    ArgumentCaptor<LedgerEntry> debitCaptor = forClass(LedgerEntry.class);
    ArgumentCaptor<LedgerEntry> creditCaptor = forClass(LedgerEntry.class);

    engine.postDoubleEntry(
        DEBT_ID,
        TestAccountCode.RECEIVABLES,
        TestAccountCode.COLLECTION_REVENUE,
        AMOUNT,
        EFFECTIVE_DATE,
        "REF-001",
        "Debt registration",
        EntryCategory.DEBT_REGISTRATION);

    verify(ledgerEntryStore).saveDoubleEntry(debitCaptor.capture(), creditCaptor.capture());
    assertThat(debitCaptor.getValue().getTransactionId())
        .isNotNull()
        .isEqualTo(creditCaptor.getValue().getTransactionId());
  }

  @Test
  void givenValidAmount_whenPostDoubleEntry_thenFirstEntryIsDebitAndSecondIsCredit() {
    // Ref: BookkeepingEngineImpl — DEBIT entry first, CREDIT entry second
    ArgumentCaptor<LedgerEntry> debitCaptor = forClass(LedgerEntry.class);
    ArgumentCaptor<LedgerEntry> creditCaptor = forClass(LedgerEntry.class);

    engine.postDoubleEntry(
        DEBT_ID,
        TestAccountCode.RECEIVABLES,
        TestAccountCode.COLLECTION_REVENUE,
        AMOUNT,
        EFFECTIVE_DATE,
        "REF-001",
        "Debt registration",
        EntryCategory.DEBT_REGISTRATION);

    verify(ledgerEntryStore).saveDoubleEntry(debitCaptor.capture(), creditCaptor.capture());
    assertThat(debitCaptor.getValue().getEntryType()).isEqualTo(EntryType.DEBIT);
    assertThat(creditCaptor.getValue().getEntryType()).isEqualTo(EntryType.CREDIT);
  }

  @Test
  void givenAnyAmount_whenPostDoubleEntry_thenBothLedgerEntriesHaveEqualAmounts() {
    // Ref: BookkeepingEngineImpl — balance invariant: debit.amount == credit.amount
    ArgumentCaptor<LedgerEntry> debitCaptor = forClass(LedgerEntry.class);
    ArgumentCaptor<LedgerEntry> creditCaptor = forClass(LedgerEntry.class);

    engine.postDoubleEntry(
        DEBT_ID,
        TestAccountCode.RECEIVABLES,
        TestAccountCode.COLLECTION_REVENUE,
        AMOUNT,
        EFFECTIVE_DATE,
        "REF-001",
        "Debt registration",
        EntryCategory.DEBT_REGISTRATION);

    verify(ledgerEntryStore).saveDoubleEntry(debitCaptor.capture(), creditCaptor.capture());
    assertThat(debitCaptor.getValue().getAmount())
        .isEqualByComparingTo(creditCaptor.getValue().getAmount());
  }

  @Test
  void givenEffectiveDate_whenPostDoubleEntry_thenEffectiveDatePropagatedToBothEntries() {
    // Ref: BookkeepingEngineImpl — effectiveDate comes from the caller, not derived
    ArgumentCaptor<LedgerEntry> debitCaptor = forClass(LedgerEntry.class);
    ArgumentCaptor<LedgerEntry> creditCaptor = forClass(LedgerEntry.class);
    LocalDate specificDate = LocalDate.of(2024, 3, 15);

    engine.postDoubleEntry(
        DEBT_ID,
        TestAccountCode.RECEIVABLES,
        TestAccountCode.COLLECTION_REVENUE,
        AMOUNT,
        specificDate,
        "REF-002",
        "Test",
        EntryCategory.DEBT_REGISTRATION);

    verify(ledgerEntryStore).saveDoubleEntry(debitCaptor.capture(), creditCaptor.capture());
    assertThat(debitCaptor.getValue().getEffectiveDate()).isEqualTo(specificDate);
    assertThat(creditCaptor.getValue().getEffectiveDate()).isEqualTo(specificDate);
  }

  @Test
  void givenPostingDate_whenPostDoubleEntry_thenPostingDateIsToday() {
    // Ref: BookkeepingEngineImpl — postingDate = LocalDate.now() (recording date)
    ArgumentCaptor<LedgerEntry> debitCaptor = forClass(LedgerEntry.class);
    ArgumentCaptor<LedgerEntry> creditCaptor = forClass(LedgerEntry.class);
    LocalDate today = LocalDate.now();

    engine.postDoubleEntry(
        DEBT_ID,
        TestAccountCode.RECEIVABLES,
        TestAccountCode.COLLECTION_REVENUE,
        AMOUNT,
        EFFECTIVE_DATE,
        "REF-003",
        "Test",
        EntryCategory.DEBT_REGISTRATION);

    verify(ledgerEntryStore).saveDoubleEntry(debitCaptor.capture(), creditCaptor.capture());
    assertThat(debitCaptor.getValue().getPostingDate()).isEqualTo(today);
    assertThat(creditCaptor.getValue().getPostingDate()).isEqualTo(today);
  }

  @Test
  void givenDebitAccount_whenPostDoubleEntry_thenDebitEntryUsesDebitAccountCode() {
    // Ref: BookkeepingEngineImpl — debitAccount maps to debitEntry.accountCode/accountName
    ArgumentCaptor<LedgerEntry> debitCaptor = forClass(LedgerEntry.class);
    ArgumentCaptor<LedgerEntry> creditCaptor = forClass(LedgerEntry.class);

    engine.postDoubleEntry(
        DEBT_ID,
        TestAccountCode.RECEIVABLES,
        TestAccountCode.COLLECTION_REVENUE,
        AMOUNT,
        EFFECTIVE_DATE,
        "REF-004",
        "Test",
        EntryCategory.DEBT_REGISTRATION);

    verify(ledgerEntryStore).saveDoubleEntry(debitCaptor.capture(), creditCaptor.capture());
    assertThat(debitCaptor.getValue().getAccountCode())
        .isEqualTo(TestAccountCode.RECEIVABLES.getCode());
    assertThat(debitCaptor.getValue().getAccountName())
        .isEqualTo(TestAccountCode.RECEIVABLES.getName());
  }

  @Test
  void givenCreditAccount_whenPostDoubleEntry_thenCreditEntryUsesCreditAccountCode() {
    // Ref: BookkeepingEngineImpl — creditAccount maps to creditEntry.accountCode/accountName
    ArgumentCaptor<LedgerEntry> debitCaptor = forClass(LedgerEntry.class);
    ArgumentCaptor<LedgerEntry> creditCaptor = forClass(LedgerEntry.class);

    engine.postDoubleEntry(
        DEBT_ID,
        TestAccountCode.RECEIVABLES,
        TestAccountCode.COLLECTION_REVENUE,
        AMOUNT,
        EFFECTIVE_DATE,
        "REF-005",
        "Test",
        EntryCategory.DEBT_REGISTRATION);

    verify(ledgerEntryStore).saveDoubleEntry(debitCaptor.capture(), creditCaptor.capture());
    assertThat(creditCaptor.getValue().getAccountCode())
        .isEqualTo(TestAccountCode.COLLECTION_REVENUE.getCode());
    assertThat(creditCaptor.getValue().getAccountName())
        .isEqualTo(TestAccountCode.COLLECTION_REVENUE.getName());
  }

  @Test
  void givenCategory_whenPostDoubleEntry_thenCategoryPropagatedToBothEntries() {
    // Ref: BookkeepingEngineImpl — category is stored on both entries for reporting
    ArgumentCaptor<LedgerEntry> debitCaptor = forClass(LedgerEntry.class);
    ArgumentCaptor<LedgerEntry> creditCaptor = forClass(LedgerEntry.class);

    engine.postDoubleEntry(
        DEBT_ID,
        TestAccountCode.INTEREST_RECEIVABLE,
        TestAccountCode.INTEREST_REVENUE,
        new BigDecimal("821.92"),
        EFFECTIVE_DATE,
        "INT-Q4",
        "Interest accrual",
        EntryCategory.INTEREST_ACCRUAL);

    verify(ledgerEntryStore).saveDoubleEntry(debitCaptor.capture(), creditCaptor.capture());
    assertThat(debitCaptor.getValue().getEntryCategory()).isEqualTo(EntryCategory.INTEREST_ACCRUAL);
    assertThat(creditCaptor.getValue().getEntryCategory()).isEqualTo(EntryCategory.INTEREST_ACCRUAL);
  }

  @Test
  void givenPostDoubleEntry_whenCalled_thenReturnsNonNullTransactionId() {
    UUID result =
        engine.postDoubleEntry(
            DEBT_ID,
            TestAccountCode.BANK,
            TestAccountCode.RECEIVABLES,
            AMOUNT,
            EFFECTIVE_DATE,
            "PAY-001",
            "Payment received",
            EntryCategory.PAYMENT);

    assertThat(result).isNotNull();
  }

  // ---------------------------------------------------------------------------
  // recordEvent
  // ---------------------------------------------------------------------------

  @Test
  void givenEvent_whenRecordEvent_thenEventSavedToFinancialEventStore() {
    // Ref: BookkeepingEngineImpl.recordEvent — delegates to financialEventStore.save
    UUID txnId = UUID.randomUUID();
    LocalDate eventDate = LocalDate.of(2025, 11, 1);
    BigDecimal amount = new BigDecimal("50000");

    engine.recordEvent(
        DEBT_ID, EventType.DEBT_REGISTERED, eventDate, amount, "REF-006", "Debt registered", txnId);

    ArgumentCaptor<FinancialEvent> captor = forClass(FinancialEvent.class);
    verify(financialEventStore).save(captor.capture());

    FinancialEvent saved = captor.getValue();
    assertThat(saved.getDebtId()).isEqualTo(DEBT_ID);
    assertThat(saved.getEventType()).isEqualTo(EventType.DEBT_REGISTERED);
    assertThat(saved.getEffectiveDate()).isEqualTo(eventDate);
    assertThat(saved.getAmount()).isEqualByComparingTo(amount);
    assertThat(saved.getLedgerTransactionId()).isEqualTo(txnId);
  }

  @Test
  void givenEvent_whenRecordEvent_thenReferenceAndDescriptionPreserved() {
    engine.recordEvent(
        DEBT_ID,
        EventType.PAYMENT_RECEIVED,
        EFFECTIVE_DATE,
        AMOUNT,
        "CREMUL-007",
        "Payment via CREMUL",
        UUID.randomUUID());

    ArgumentCaptor<FinancialEvent> captor = forClass(FinancialEvent.class);
    verify(financialEventStore).save(captor.capture());

    FinancialEvent saved = captor.getValue();
    assertThat(saved.getReference()).isEqualTo("CREMUL-007");
    assertThat(saved.getDescription()).isEqualTo("Payment via CREMUL");
  }
}
