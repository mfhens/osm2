package dk.ufst.bookkeeping.port;

import java.util.List;
import java.util.UUID;

import dk.ufst.bookkeeping.domain.FinancialEvent;

public interface FinancialEventStore {

  FinancialEvent save(FinancialEvent event);

  List<FinancialEvent> findPrincipalAffectingEvents(UUID debtId);

  List<FinancialEvent> findByDebtIdOrderByEffectiveDateAscCreatedAtAsc(UUID debtId);
}
