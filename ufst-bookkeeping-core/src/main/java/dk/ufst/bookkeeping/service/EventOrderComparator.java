package dk.ufst.bookkeeping.service;

import java.util.Comparator;
import java.util.Map;

import dk.ufst.bookkeeping.domain.EventType;
import dk.ufst.bookkeeping.domain.FinancialEvent;

/**
 * Deterministic ordering for financial events. Primary sort: effective date ascending. Secondary
 * sort (same effective date): event type priority. Tertiary sort: created_at ascending (posting
 * order).
 *
 * <p>Priority order for same-date events: DEBT_REGISTERED first, then balance-reducing events
 * (payments, offsetting), then corrections, then interest accruals last.
 */
public final class EventOrderComparator implements Comparator<FinancialEvent> {

  private static final Map<EventType, Integer> TYPE_PRIORITY =
      Map.ofEntries(
          Map.entry(EventType.DEBT_REGISTERED, 0),
          Map.entry(EventType.UDLAEG_REGISTERED, 1),
          Map.entry(EventType.PAYMENT_RECEIVED, 2),
          Map.entry(EventType.OFFSETTING_EXECUTED, 3),
          Map.entry(EventType.WRITE_OFF, 4),
          Map.entry(EventType.REFUND, 5),
          Map.entry(EventType.UDLAEG_CORRECTED, 6),
          Map.entry(EventType.CORRECTION, 7),
          Map.entry(EventType.COVERAGE_REVERSED, 8),
          Map.entry(EventType.INTEREST_ACCRUED, 9));

  public static final EventOrderComparator INSTANCE = new EventOrderComparator();

  private EventOrderComparator() {}

  @Override
  public int compare(FinancialEvent a, FinancialEvent b) {
    int dateCompare = a.getEffectiveDate().compareTo(b.getEffectiveDate());
    if (dateCompare != 0) {
      return dateCompare;
    }

    int priorityA = TYPE_PRIORITY.getOrDefault(a.getEventType(), 99);
    int priorityB = TYPE_PRIORITY.getOrDefault(b.getEventType(), 99);
    int typeCompare = Integer.compare(priorityA, priorityB);
    if (typeCompare != 0) {
      return typeCompare;
    }

    if (a.getCreatedAt() != null && b.getCreatedAt() != null) {
      return a.getCreatedAt().compareTo(b.getCreatedAt());
    }
    return 0;
  }
}
