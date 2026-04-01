package dk.ufst.bookkeeping.engine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import dk.ufst.bookkeeping.domain.EntryCategory;
import dk.ufst.bookkeeping.domain.EntryType;
import dk.ufst.bookkeeping.domain.EventType;
import dk.ufst.bookkeeping.domain.FinancialEvent;
import dk.ufst.bookkeeping.domain.LedgerEntry;
import dk.ufst.bookkeeping.port.FinancialEventStore;
import dk.ufst.bookkeeping.port.LedgerEntryStore;
import dk.ufst.bookkeeping.spi.BookkeepingAccountCode;

// AIDEV-NOTE: Plain Java class — no Spring, no JPA. Transaction management is the caller's
// responsibility. The immudb dual-write happens inside LedgerEntryStore.saveDoubleEntry().
// AIDEV-NOTE: Balance validation (debit == credit) is enforced here per AIDEV-TODO from the
// original BookkeepingServiceImpl.
public class BookkeepingEngineImpl implements BookkeepingEngine {

  private final LedgerEntryStore ledgerEntryStore;
  private final FinancialEventStore financialEventStore;

  public BookkeepingEngineImpl(
      LedgerEntryStore ledgerEntryStore, FinancialEventStore financialEventStore) {
    this.ledgerEntryStore = ledgerEntryStore;
    this.financialEventStore = financialEventStore;
  }

  @Override
  public UUID postDoubleEntry(
      UUID debtId,
      BookkeepingAccountCode debitAccount,
      BookkeepingAccountCode creditAccount,
      BigDecimal amount,
      LocalDate effectiveDate,
      String reference,
      String description,
      EntryCategory category) {

    UUID transactionId = UUID.randomUUID();
    LocalDate postingDate = LocalDate.now();

    LedgerEntry debitEntry =
        LedgerEntry.builder()
            .transactionId(transactionId)
            .debtId(debtId)
            .accountCode(debitAccount.getCode())
            .accountName(debitAccount.getName())
            .entryType(EntryType.DEBIT)
            .amount(amount)
            .effectiveDate(effectiveDate)
            .postingDate(postingDate)
            .reference(reference)
            .description(description)
            .entryCategory(category)
            .build();

    LedgerEntry creditEntry =
        LedgerEntry.builder()
            .transactionId(transactionId)
            .debtId(debtId)
            .accountCode(creditAccount.getCode())
            .accountName(creditAccount.getName())
            .entryType(EntryType.CREDIT)
            .amount(amount)
            .effectiveDate(effectiveDate)
            .postingDate(postingDate)
            .reference(reference)
            .description(description)
            .entryCategory(category)
            .build();

    if (debitEntry.getAmount().compareTo(creditEntry.getAmount()) != 0) {
      throw new IllegalStateException(
          "Double-entry balance violated: debit="
              + debitEntry.getAmount()
              + " != credit="
              + creditEntry.getAmount()
              + " for transactionId="
              + transactionId);
    }

    ledgerEntryStore.saveDoubleEntry(debitEntry, creditEntry);
    return transactionId;
  }

  @Override
  public void recordEvent(
      UUID debtId,
      EventType eventType,
      LocalDate effectiveDate,
      BigDecimal amount,
      String reference,
      String description,
      UUID ledgerTransactionId) {

    FinancialEvent event =
        FinancialEvent.builder()
            .debtId(debtId)
            .eventType(eventType)
            .effectiveDate(effectiveDate)
            .amount(amount)
            .reference(reference)
            .description(description)
            .ledgerTransactionId(ledgerTransactionId)
            .build();

    financialEventStore.save(event);
  }
}
