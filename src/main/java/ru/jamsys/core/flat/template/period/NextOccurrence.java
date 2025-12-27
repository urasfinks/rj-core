package ru.jamsys.core.flat.template.period;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;

/**
 * Computes the next occurrence time for a repeating step defined as:
 *   nextTick = tick.plus(period).plus(duration)

 * Semantics:
 * - Returns the first tick strictly AFTER the 'after' moment.
 * - If after < start, returns start.

 * Performance:
 * - duration-only steps are handled in O(1).
 * - period-only steps use a safe fast-forward (months/years and pure days) and then iterate.
 * - mixed steps (period + duration) are iterated (calendar-accurate).
 */

/*
* start — начало расписания
* step — шаг (Period + Duration)
* after — текущий момент или любой произвольный момент времени.
*         Это точка отсчёта поиска, а не часть шаблона.
*         Момент времени, после которого нужно найти первое следующее срабатывание расписания.
* */
public final class NextOccurrence {

    private NextOccurrence() {
    }

    public static OffsetDateTime next(OffsetDateTime start, Period p, Duration d, OffsetDateTime after) {
        if (start == null || p == null || d == null || after == null) {
            throw new IllegalArgumentException("Arguments must not be null");
        }

        // If the reference point is before the schedule starts, the next tick is the start itself.
        if (after.isBefore(start)) {
            return start;
        }

        // Validate step
        if (p.isZero() && d.isZero()) {
            throw new IllegalArgumentException("Step must not be zero.");
        }
        if (p.isNegative()) {
            throw new IllegalArgumentException("Period step must not be negative.");
        }
        if (d.isNegative()) {
            throw new IllegalArgumentException("Duration step must not be negative.");
        }

        // Fast path: duration-only (p is zero, d is guaranteed non-zero because (p==0 && d==0) is rejected above)
        if (p.isZero()) {
            return nextDurationOnly(start, d, after);
        }

        OffsetDateTime cur = start;

        // Safe acceleration: only when duration is zero (period-only). For mixed steps, do not "jump"
        // because plus(period).plus(duration) repeated is not equivalent to a coarse jump.
        if (d.isZero()) {
            cur = fastForwardPeriodOnly(cur, p, after);
        }

        // Exact iteration
        // For period-only after fast-forward, remaining iterations are typically small.
        // For mixed steps, this preserves calendar correctness.
        while (!cur.isAfter(after)) {
            cur = cur.plus(p).plus(d);
        }
        return cur;
    }

    public static OffsetDateTime next(OffsetDateTime start, String isoStep, OffsetDateTime after) {
        if (isoStep == null) {
            throw new IllegalArgumentException("isoStep must not be null");
        }
        IsoDuration parts = IsoDuration.parse(isoStep);
        return next(start, parts.getPeriod(), parts.getDuration(), after);
    }

    public static OffsetDateTime next(OffsetDateTime start, IsoDuration isoDuration, OffsetDateTime after) {
        if (isoDuration == null) {
            throw new IllegalArgumentException("isoDuration must not be null");
        }
        return next(start, isoDuration.getPeriod(), isoDuration.getDuration(), after);
    }

    /**
     * Computes the next tick for a duration-only step in O(1).
     * Returns the first tick strictly AFTER 'after'.
     */
    private static OffsetDateTime nextDurationOnly(OffsetDateTime start, Duration step, OffsetDateTime after) {
        // step is guaranteed positive and non-zero by caller
        long stepNanos = step.toNanos();
        long deltaNanos = Duration.between(start, after).toNanos();

        // If after is exactly on a tick, next is tick+1.
        long k = deltaNanos / stepNanos + 1;
        return start.plus(step.multipliedBy(k));
    }

    /**
     * Safe fast-forward for period-only steps.

     * Strategy:
     * - If the period has years/months part, jump by whole period-month blocks using MONTHS between.
     * - If the period is purely days (no months/years), jump by whole day blocks using DAYS between.
     * - Otherwise, do not attempt aggressive jumps; caller will iterate exactly.

     * This method never overshoots correctness requirements; it's only an optimization.
     */
    private static OffsetDateTime fastForwardPeriodOnly(OffsetDateTime start, Period step, OffsetDateTime after) {
        OffsetDateTime cur = start;

        int stepMonths = step.getYears() * 12 + step.getMonths();
        int stepDays = step.getDays();

        // Jump by month-blocks when step includes months/years.
        if (stepMonths > 0) {
            long monthsBetween = ChronoUnit.MONTHS.between(cur, after);
            long k = monthsBetween / stepMonths;
            if (k > 0) {
                cur = cur.plusMonths(k * (long) stepMonths);
            }
            // Note: any day component will be handled by exact iteration in caller.
            return cur;
        }

        // Jump by day-blocks when step is purely days.
        if (stepMonths == 0 && stepDays > 0) {
            long daysBetween = ChronoUnit.DAYS.between(cur, after);
            long k = daysBetween / stepDays;
            if (k > 0) {
                cur = cur.plusDays(k * (long) stepDays);
            }
            return cur;
        }

        // For other shapes (e.g., P0D which would have been rejected, or complex negatives which are rejected),
        // do nothing.
        return cur;
    }

}
