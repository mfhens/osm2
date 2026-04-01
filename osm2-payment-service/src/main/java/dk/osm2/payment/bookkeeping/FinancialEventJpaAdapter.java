package dk.osm2.payment.bookkeeping;

import dk.osm2.payment.entity.FinancialEventEntity;
import dk.osm2.payment.repository.FinancialEventRepository;
import dk.ufst.bookkeeping.domain.FinancialEvent;
import dk.ufst.bookkeeping.port.FinancialEventStore;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * JPA adapter for {@link FinancialEventStore}.
 *
 * <p>After persisting a new event the generated {@code id} and Hibernate-populated {@code
 * createdAt} are mapped back to the returned domain object so callers receive the authoritative
 * values set by the database.
 */
@RequiredArgsConstructor
public class FinancialEventJpaAdapter implements FinancialEventStore {

  private final FinancialEventRepository repository;

  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public FinancialEvent save(FinancialEvent event) {
    FinancialEventEntity saved = repository.save(toEntity(event));
    return toDomain(saved);
  }

  @Override
  public List<FinancialEvent> findPrincipalAffectingEvents(UUID debtId) {
    return repository.findPrincipalAffectingEvents(debtId).stream().map(this::toDomain).toList();
  }

  @Override
  public List<FinancialEvent> findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(UUID debtId) {
    return repository.findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(debtId).stream()
        .map(this::toDomain)
        .toList();
  }

  private FinancialEventEntity toEntity(FinancialEvent d) {
    return FinancialEventEntity.builder()
        .debtId(d.getDebtId())
        .eventType(d.getEventType())
        .effectiveDate(d.getEffectiveDate())
        .amount(d.getAmount())
        .correctsEventId(d.getCorrectsEventId())
        .reference(d.getReference())
        .description(d.getDescription())
        .ledgerTransactionId(d.getLedgerTransactionId())
        .build();
  }

  private FinancialEvent toDomain(FinancialEventEntity e) {
    return FinancialEvent.builder()
        .id(e.getId())
        .debtId(e.getDebtId())
        .eventType(e.getEventType())
        .effectiveDate(e.getEffectiveDate())
        .amount(e.getAmount())
        .correctsEventId(e.getCorrectsEventId())
        .reference(e.getReference())
        .description(e.getDescription())
        .ledgerTransactionId(e.getLedgerTransactionId())
        .createdAt(e.getCreatedAt())
        .build();
  }
}
