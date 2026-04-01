package dk.ufst.bookkeeping.port;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import dk.ufst.bookkeeping.domain.LedgerEntry;

public interface LedgerEntryStore {

  void saveDoubleEntry(LedgerEntry debitEntry, LedgerEntry creditEntry);

  void saveSingle(LedgerEntry entry);

  List<LedgerEntry> findInterestAccrualsAfterDate(UUID debtId, LocalDate fromDate);

  List<LedgerEntry> findActiveEntriesByDebtId(UUID debtId);

  List<LedgerEntry> findByTransactionId(UUID transactionId);

  boolean existsByReversalOfTransactionId(UUID transactionId);
}
