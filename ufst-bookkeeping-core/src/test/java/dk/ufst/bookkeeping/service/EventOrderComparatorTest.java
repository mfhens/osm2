package dk.ufst.bookkeeping.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dk.ufst.bookkeeping.domain.EventType;
import dk.ufst.bookkeeping.domain.FinancialEvent;

/**
 * Unit tests for EventOrderComparator.
 *
 * <p>Covers: primary sort by effective_date (earlier first), secondary sort by event-type priority
 * (DEBT_REGISTERED before payments, interest last), and tertiary sort by createdAt when both date
 * and type are equal.
 */
class EventOrderComparatorTest {

  private static final EventOrderComparator COMPARATOR = EventOrderComparator.INSTANCE;

  // ---------------------------------------------------------------------------
  // Primary sort: effective date
  // ---------------------------------------------------------------------------

  @Test
  void givenDifferentDates_whenCompare_thenEarlierDateSortsFirst() {
    // Ref: EventOrderComparator — primary key: effectiveDate ascending
    FinancialEvent early = buildEvent(LocalDate.of(2025, 1, 1), EventType.DEBT_REGISTERED, null);
    FinancialEvent late = buildEvent(LocalDate.of(2025, 6, 1), EventType.DEBT_REGISTERED, null);

    assertThat(COMPARATOR.compare(early, late)).isLessThan(0);
    assertThat(COMPARATOR.compare(late, early)).isGreaterThan(0);
  }

  @Test
  void givenSameDate_whenCompare_thenDateComparisonIsZeroBeforeTiebreak() {
    // Ref: EventOrderComparator — equal dates fall through to type-priority tiebreak
    LocalDate same = LocalDate.of(2025, 3, 15);
    FinancialEvent a = buildEvent(same, EventType.DEBT_REGISTERED, null);
    FinancialEvent b = buildEvent(same, EventType.DEBT_REGISTERED, null);

    // Same date, same type, no createdAt → comparator returns 0
    assertThat(COMPARATOR.compare(a, b)).isEqualTo(0);
  }

  // ---------------------------------------------------------------------------
  // Secondary sort: event-type priority
  // ---------------------------------------------------------------------------

  @Test
  void givenSameDate_whenCompare_thenDebtRegisteredSortsBeforePaymentReceived() {
    // Ref: EventOrderComparator — DEBT_REGISTERED priority 0, PAYMENT_RECEIVED priority 2
    LocalDate date = LocalDate.of(2025, 6, 1);
    FinancialEvent debt = buildEvent(date, EventType.DEBT_REGISTERED, null);
    FinancialEvent payment = buildEvent(date, EventType.PAYMENT_RECEIVED, null);

    assertThat(COMPARATOR.compare(debt, payment)).isLessThan(0);
    assertThat(COMPARATOR.compare(payment, debt)).isGreaterThan(0);
  }

  @Test
  void givenSameDate_whenCompare_thenInterestAccruedSortsLast() {
    // Ref: EventOrderComparator — INTEREST_ACCRUED priority 9 (highest number = last)
    LocalDate date = LocalDate.of(2025, 6, 1);
    FinancialEvent interest = buildEvent(date, EventType.INTEREST_ACCRUED, null);
    FinancialEvent payment = buildEvent(date, EventType.PAYMENT_RECEIVED, null);
    FinancialEvent correction = buildEvent(date, EventType.CORRECTION, null);

    assertThat(COMPARATOR.compare(interest, payment)).isGreaterThan(0);
    assertThat(COMPARATOR.compare(interest, correction)).isGreaterThan(0);
  }

  @Test
  void givenSameDate_whenCompare_thenPaymentSortsBeforeCorrection() {
    // Ref: EventOrderComparator — PAYMENT_RECEIVED priority 2, CORRECTION priority 7
    LocalDate date = LocalDate.of(2025, 9, 1);
    FinancialEvent payment = buildEvent(date, EventType.PAYMENT_RECEIVED, null);
    FinancialEvent correction = buildEvent(date, EventType.CORRECTION, null);

    assertThat(COMPARATOR.compare(payment, correction)).isLessThan(0);
  }

  @Test
  void givenSameDate_whenCompare_thenUdlaegRegisteredSortsAfterDebtRegistered() {
    // Ref: EventOrderComparator — DEBT_REGISTERED priority 0, UDLAEG_REGISTERED priority 1
    LocalDate date = LocalDate.of(2025, 4, 1);
    FinancialEvent debt = buildEvent(date, EventType.DEBT_REGISTERED, null);
    FinancialEvent udlaeg = buildEvent(date, EventType.UDLAEG_REGISTERED, null);

    assertThat(COMPARATOR.compare(debt, udlaeg)).isLessThan(0);
  }

  @Test
  void givenSameDate_whenCompare_thenWriteOffSortsAfterPayment() {
    // Ref: EventOrderComparator — PAYMENT_RECEIVED priority 2, WRITE_OFF priority 4
    LocalDate date = LocalDate.of(2025, 5, 15);
    FinancialEvent payment = buildEvent(date, EventType.PAYMENT_RECEIVED, null);
    FinancialEvent writeOff = buildEvent(date, EventType.WRITE_OFF, null);

    assertThat(COMPARATOR.compare(payment, writeOff)).isLessThan(0);
  }

  // ---------------------------------------------------------------------------
  // Full priority ordering when sorted as a list
  // ---------------------------------------------------------------------------

  @Test
  void givenAllEventTypesOnSameDate_whenSorted_thenDebtRegisteredIsFirst() {
    // Ref: EventOrderComparator — DEBT_REGISTERED has the lowest priority number (0)
    LocalDate date = LocalDate.of(2025, 7, 1);
    List<FinancialEvent> events = new ArrayList<>();
    events.add(buildEvent(date, EventType.INTEREST_ACCRUED, null));
    events.add(buildEvent(date, EventType.CORRECTION, null));
    events.add(buildEvent(date, EventType.PAYMENT_RECEIVED, null));
    events.add(buildEvent(date, EventType.DEBT_REGISTERED, null));
    events.add(buildEvent(date, EventType.WRITE_OFF, null));

    Collections.sort(events, COMPARATOR);

    assertThat(events.get(0).getEventType()).isEqualTo(EventType.DEBT_REGISTERED);
  }

  @Test
  void givenAllEventTypesOnSameDate_whenSorted_thenInterestAccruedIsLast() {
    // Ref: EventOrderComparator — INTEREST_ACCRUED has the highest priority number (9) → last
    LocalDate date = LocalDate.of(2025, 7, 1);
    List<FinancialEvent> events = new ArrayList<>();
    events.add(buildEvent(date, EventType.INTEREST_ACCRUED, null));
    events.add(buildEvent(date, EventType.DEBT_REGISTERED, null));
    events.add(buildEvent(date, EventType.PAYMENT_RECEIVED, null));
    events.add(buildEvent(date, EventType.WRITE_OFF, null));

    Collections.sort(events, COMPARATOR);

    assertThat(events.get(events.size() - 1).getEventType()).isEqualTo(EventType.INTEREST_ACCRUED);
  }

  @Test
  void givenMixedDatesAndTypes_whenSorted_thenChronologicalOrderTakesPrecedence() {
    // Ref: EventOrderComparator — date is primary; type priority only breaks ties on same date.
    // INTEREST_ACCRUED on Jan 1 must sort before DEBT_REGISTERED on Feb 1.
    FinancialEvent earlyInterest =
        buildEvent(LocalDate.of(2025, 1, 1), EventType.INTEREST_ACCRUED, null);
    FinancialEvent lateDebt =
        buildEvent(LocalDate.of(2025, 2, 1), EventType.DEBT_REGISTERED, null);

    assertThat(COMPARATOR.compare(earlyInterest, lateDebt)).isLessThan(0);
  }

  // ---------------------------------------------------------------------------
  // Tertiary sort: createdAt
  // ---------------------------------------------------------------------------

  @Test
  void givenSameDateAndType_whenCompare_thenEarlierCreatedAtSortsFirst() {
    // Ref: EventOrderComparator — when date and type tie, createdAt ascending is tertiary key
    LocalDate date = LocalDate.of(2025, 8, 1);
    LocalDateTime earlier = LocalDateTime.of(2025, 8, 1, 10, 0);
    LocalDateTime later = LocalDateTime.of(2025, 8, 1, 11, 0);

    FinancialEvent first = buildEventWithCreatedAt(date, EventType.PAYMENT_RECEIVED, earlier);
    FinancialEvent second = buildEventWithCreatedAt(date, EventType.PAYMENT_RECEIVED, later);

    assertThat(COMPARATOR.compare(first, second)).isLessThan(0);
    assertThat(COMPARATOR.compare(second, first)).isGreaterThan(0);
  }

  @Test
  void givenNullCreatedAt_whenCompare_thenNoExceptionAndStableOrder() {
    // Ref: EventOrderComparator — null createdAt is handled gracefully (returns 0)
    LocalDate date = LocalDate.of(2025, 9, 1);
    FinancialEvent a = buildEvent(date, EventType.PAYMENT_RECEIVED, null); // createdAt = null
    FinancialEvent b = buildEvent(date, EventType.PAYMENT_RECEIVED, null); // createdAt = null

    // Must not throw; both are null → treated as equal
    assertThat(COMPARATOR.compare(a, b)).isEqualTo(0);
  }

  @Test
  void givenOneNullAndOneNonNullCreatedAt_whenCompare_thenNoException() {
    // Ref: EventOrderComparator — mixed null/non-null createdAt is handled gracefully
    LocalDate date = LocalDate.of(2025, 9, 1);
    FinancialEvent withTime =
        buildEventWithCreatedAt(date, EventType.REFUND, LocalDateTime.of(2025, 9, 1, 8, 0));
    FinancialEvent withoutTime = buildEvent(date, EventType.REFUND, null);

    // null check guard: if either is null → returns 0 (tie)
    assertThat(COMPARATOR.compare(withTime, withoutTime)).isEqualTo(0);
    assertThat(COMPARATOR.compare(withoutTime, withTime)).isEqualTo(0);
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private FinancialEvent buildEvent(LocalDate date, EventType type, LocalDateTime createdAt) {
    return FinancialEvent.builder()
        .id(UUID.randomUUID())
        .debtId(UUID.randomUUID())
        .eventType(type)
        .effectiveDate(date)
        .amount(BigDecimal.TEN)
        .createdAt(createdAt)
        .build();
  }

  private FinancialEvent buildEventWithCreatedAt(
      LocalDate date, EventType type, LocalDateTime createdAt) {
    return buildEvent(date, type, createdAt);
  }
}
