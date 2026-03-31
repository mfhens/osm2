package dk.osm2.registration.service;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Month;

/**
 * Stateless service implementing the effective-date calculation rules for OSS registration.
 *
 * <p>Rule 1 — Quarter-start rule (ML § 66b stk. 1):
 * The effective date is the first day of the calendar quarter following the notification date.
 *
 * <p>Rule 2 — Early-delivery exception (ML § 66b stk. 3):
 * If the taxable person made a first eligible delivery and submitted the registration notification
 * by the 10th day of the month following that delivery, the effective date is the first delivery date.
 *
 * <p>Petition: OSS-02 — FR-OSS-02-003, FR-OSS-02-004, FR-OSS-02-005
 */
@Service
public class EffectiveDateCalculationService {

    /**
     * Calculate the effective date for a registration.
     *
     * @param notificationDate  the date the registration notification was submitted
     * @param firstDeliveryDate the date of the first eligible delivery, or null if none
     * @return the calculated effective date
     */
    public LocalDate calculate(LocalDate notificationDate, @Nullable LocalDate firstDeliveryDate) {
        if (firstDeliveryDate != null && qualifiesForEarlyDeliveryException(notificationDate, firstDeliveryDate)) {
            return firstDeliveryDate;
        }
        return firstDayOfNextQuarter(notificationDate);
    }

    /**
     * Determine whether the early-delivery exception applies.
     *
     * <p>The exception applies when the notification is submitted by the 10th day
     * of the month following the first delivery (ML § 66b stk. 3).
     *
     * <p>Example: delivery on 2024-02-08 → 10th day of following month = 2024-03-10.
     * If notification date ≤ 2024-03-10, exception applies and effective date = 2024-02-08.
     *
     * @param notificationDate  the date the notification was submitted
     * @param firstDeliveryDate the date of the first eligible delivery
     * @return true if the exception applies
     */
    boolean qualifiesForEarlyDeliveryException(LocalDate notificationDate, LocalDate firstDeliveryDate) {
        // The deadline is the 10th day of the month following the first delivery
        LocalDate tenthDayOfFollowingMonth = firstDeliveryDate.plusMonths(1).withDayOfMonth(10);
        // Exception applies if notification was submitted on or before the deadline
        return !notificationDate.isAfter(tenthDayOfFollowingMonth);
    }

    /**
     * Return the first day of the calendar quarter following the given date.
     *
     * <p>Quarters: Q1 = Jan–Mar, Q2 = Apr–Jun, Q3 = Jul–Sep, Q4 = Oct–Dec.
     * If the date is already on the first day of a quarter, the NEXT quarter is returned
     * (there is no "same-quarter" exception in the regulation).
     *
     * @param date any date
     * @return first day of the next quarter
     */
    LocalDate firstDayOfNextQuarter(LocalDate date) {
        int month = date.getMonthValue();
        // Determine the first month of the current quarter: 1, 4, 7, or 10
        int currentQuarterFirstMonth = ((month - 1) / 3) * 3 + 1;
        // Next quarter starts 3 months after the current quarter's first month
        int nextQuarterFirstMonth = currentQuarterFirstMonth + 3;
        int year = date.getYear();
        if (nextQuarterFirstMonth > 12) {
            nextQuarterFirstMonth -= 12;
            year++;
        }
        return LocalDate.of(year, nextQuarterFirstMonth, 1);
    }
}
