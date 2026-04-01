package dk.osm2.payment.repository;

import dk.osm2.payment.entity.FinancialEventEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FinancialEventRepository extends JpaRepository<FinancialEventEntity, UUID> {

  /** All events for a debt ordered chronologically then by insertion time (stable sort). */
  List<FinancialEventEntity> findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(UUID debtId);

  /**
   * Events that affect the principal balance (all event types except {@code INTEREST_ACCRUED} and
   * {@code COVERAGE_REVERSED}), ordered chronologically for timeline replay.
   */
  @Query(
      """
      SELECT e FROM FinancialEventEntity e
      WHERE e.debtId = :debtId
        AND e.eventType NOT IN (
          dk.ufst.bookkeeping.domain.EventType.INTEREST_ACCRUED,
          dk.ufst.bookkeeping.domain.EventType.COVERAGE_REVERSED
        )
      ORDER BY e.effectiveDate ASC, e.createdAt ASC
      """)
  List<FinancialEventEntity> findPrincipalAffectingEvents(@Param("debtId") UUID debtId);
}
