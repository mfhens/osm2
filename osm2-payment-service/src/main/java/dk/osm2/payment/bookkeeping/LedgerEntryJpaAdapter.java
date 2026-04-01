package dk.osm2.payment.bookkeeping;

import dk.osm2.payment.entity.LedgerEntryEntity;
import dk.osm2.payment.repository.LedgerEntryRepository;
import dk.ufst.bookkeeping.domain.LedgerEntry;
import dk.ufst.bookkeeping.port.LedgerEntryStore;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA adapter for {@link LedgerEntryStore}.
 *
 * <p>Persists ledger entries to PostgreSQL and writes a tamper-evident copy to immudb on every
 * insert [ADR-0029]. All mutating methods require an active transaction ({@link
 * Propagation#MANDATORY}) — transaction management is the caller's responsibility per the contract
 * of {@code BookkeepingEngine}.
 */
@Slf4j
@RequiredArgsConstructor
public class LedgerEntryJpaAdapter implements LedgerEntryStore {

  private final LedgerEntryRepository repository;
  private final ImmudbLedgerClient immudbClient;

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void saveDoubleEntry(LedgerEntry debitEntry, LedgerEntry creditEntry) {
    repository.save(toEntity(debitEntry));
    repository.save(toEntity(creditEntry));
    immudbClient.writeLedgerEntry(debitEntry);
    immudbClient.writeLedgerEntry(creditEntry);
  }

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void saveSingle(LedgerEntry entry) {
    repository.save(toEntity(entry));
    immudbClient.writeLedgerEntry(entry);
  }

  @Override
  public List<LedgerEntry> findInterestAccrualsAfterDate(UUID debtId, LocalDate fromDate) {
    return repository.findInterestAccrualsAfterDate(debtId, fromDate).stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public List<LedgerEntry> findActiveEntriesByDebtId(UUID debtId) {
    return repository.findActiveEntriesByDebtId(debtId).stream().map(this::toDomain).toList();
  }

  @Override
  public List<LedgerEntry> findByTransactionId(UUID transactionId) {
    return repository.findByTransactionId(transactionId).stream().map(this::toDomain).toList();
  }

  @Override
  public boolean existsByReversalOfTransactionId(UUID transactionId) {
    return repository.existsByReversalOfTransactionId(transactionId);
  }

  private LedgerEntryEntity toEntity(LedgerEntry d) {
    return LedgerEntryEntity.builder()
        .transactionId(d.getTransactionId())
        .debtId(d.getDebtId())
        .accountCode(d.getAccountCode())
        .accountName(d.getAccountName())
        .entryType(d.getEntryType())
        .amount(d.getAmount())
        .effectiveDate(d.getEffectiveDate())
        .postingDate(d.getPostingDate())
        .reference(d.getReference())
        .description(d.getDescription())
        .reversalOfTransactionId(d.getReversalOfTransactionId())
        .entryCategory(d.getEntryCategory())
        .build();
  }

  private LedgerEntry toDomain(LedgerEntryEntity e) {
    return LedgerEntry.builder()
        .transactionId(e.getTransactionId())
        .debtId(e.getDebtId())
        .accountCode(e.getAccountCode())
        .accountName(e.getAccountName())
        .entryType(e.getEntryType())
        .amount(e.getAmount())
        .effectiveDate(e.getEffectiveDate())
        .postingDate(e.getPostingDate())
        .reference(e.getReference())
        .description(e.getDescription())
        .reversalOfTransactionId(e.getReversalOfTransactionId())
        .entryCategory(e.getEntryCategory())
        .build();
  }
}
