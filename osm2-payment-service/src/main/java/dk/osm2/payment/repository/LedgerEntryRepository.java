package dk.osm2.payment.repository;

import dk.osm2.payment.entity.LedgerEntryEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntryEntity, UUID> {

  List<LedgerEntryEntity> findByTransactionId(UUID transactionId);

  boolean existsByReversalOfTransactionId(UUID transactionId);

  /** Entries for INTEREST_ACCRUAL category on or after {@code fromDate} for the given debt. */
  @Query(
      """
      SELECT e FROM LedgerEntryEntity e
      WHERE e.debtId = :debtId
        AND e.entryCategory = dk.ufst.bookkeeping.domain.EntryCategory.INTEREST_ACCRUAL
        AND e.effectiveDate >= :fromDate
      """)
  List<LedgerEntryEntity> findInterestAccrualsAfterDate(
      @Param("debtId") UUID debtId, @Param("fromDate") LocalDate fromDate);

  /**
   * Entries whose {@code transactionId} has not yet been reversed by any other entry (i.e. no
   * entry exists with {@code reversalOfTransactionId = e.transactionId}).
   */
  @Query(
      """
      SELECT e FROM LedgerEntryEntity e
      WHERE e.debtId = :debtId
        AND NOT EXISTS (
          SELECT 1 FROM LedgerEntryEntity r
          WHERE r.debtId = :debtId
            AND r.reversalOfTransactionId = e.transactionId
        )
      """)
  List<LedgerEntryEntity> findActiveEntriesByDebtId(@Param("debtId") UUID debtId);
}
