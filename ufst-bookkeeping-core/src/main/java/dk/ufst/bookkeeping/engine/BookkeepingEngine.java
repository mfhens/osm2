package dk.ufst.bookkeeping.engine;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import dk.ufst.bookkeeping.domain.EntryCategory;
import dk.ufst.bookkeeping.domain.EventType;
import dk.ufst.bookkeeping.spi.BookkeepingAccountCode;

public interface BookkeepingEngine {

  /**
   * Posts a balanced double-entry pair and returns the generated transactionId.
   *
   * @return the transactionId shared by the debit and credit entries
   */
  UUID postDoubleEntry(
      UUID debtId,
      BookkeepingAccountCode debitAccount,
      BookkeepingAccountCode creditAccount,
      BigDecimal amount,
      LocalDate effectiveDate,
      String reference,
      String description,
      EntryCategory category);

  void recordEvent(
      UUID debtId,
      EventType eventType,
      LocalDate effectiveDate,
      BigDecimal amount,
      String reference,
      String description,
      UUID ledgerTransactionId);
}
