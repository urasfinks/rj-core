package ru.jamsys.core.flat.template.period;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Stateful next occurrence calculator.
 * <p>
 * - startEpochMillis is fixed at construction time.
 * - next(afterEpochMillis) returns first tick strictly AFTER afterEpochMillis.
 * - Calendar correctness is ensured via ZoneId.
 * - guardMaxIterations protects against pathological iteration.
 */
public final class PlanScheduler {

    private final long startEpochMillis;
    private final Period period;
    private final Duration duration;
    private final ZoneId zone;
    private final int guardMaxIterations;

    private OffsetDateTime cur;

    public PlanScheduler(
            long startEpochMillis,
            Iso8601 rule,
            ZoneId zone,
            int guardMaxIterations
    ) {
        this.startEpochMillis = startEpochMillis;

        Objects.requireNonNull(rule, "rule must not be null");
        this.period = Objects.requireNonNull(rule.getPeriod(), "rule.period must not be null");
        this.duration = Objects.requireNonNull(rule.getDuration(), "rule.duration must not be null");
        this.zone = Objects.requireNonNull(zone, "zone must not be null");

        if (guardMaxIterations <= 0) {
            throw new IllegalArgumentException("guardMaxIterations must be > 0");
        }
        this.guardMaxIterations = guardMaxIterations;

        // Validate step
        if (period.isZero() && duration.isZero()) {
            throw new IllegalArgumentException("Step must not be zero.");
        }
        if (period.isNegative()) {
            throw new IllegalArgumentException("Period step must not be negative.");
        }
        if (duration.isNegative()) {
            throw new IllegalArgumentException("Duration step must not be negative.");
        }
        this.cur = OffsetDateTime.ofInstant(Instant.ofEpochMilli(startEpochMillis), zone);
    }

    public PlanScheduler(long startEpochMillis, Iso8601 rule, ZoneId zone) {
        this(startEpochMillis, rule, zone, 1_000_000);
    }

    public PlanScheduler(long startEpochMillis, String isoStep, ZoneId zone, int guardMaxIterations) {
        this(
                startEpochMillis,
                Iso8601.parse(Objects.requireNonNull(isoStep, "isoStep must not be null")),
                zone,
                guardMaxIterations
        );
    }

    /**
     * @return epochMillis of the first tick strictly AFTER afterEpochMillis,
     * or startEpochMillis if afterEpochMillis < startEpochMillis.
     */
    public long next(long afterEpochMillis) {
        // If reference is before schedule start → return start
        if (afterEpochMillis < startEpochMillis) {
            return startEpochMillis;
        }

        // Fast path: duration-only (pure epoch math, O(1))
        if (period.isZero()) {
            return nextDurationOnlyEpoch(duration, afterEpochMillis);
        }

        OffsetDateTime after = OffsetDateTime.ofInstant(Instant.ofEpochMilli(afterEpochMillis), zone);

        // Safe acceleration only for period-only rules
        if (duration.isZero()) {
            cur = fastForwardPeriodOnly(cur, period, after);
        }

        int it = 0;
        while (!cur.isAfter(after)) {
            if (++it > guardMaxIterations) {
                throw new IllegalStateException(
                        "guardMaxIterations exceeded (" + guardMaxIterations + "). " +
                                "startEpochMillis=" + startEpochMillis +
                                ", afterEpochMillis=" + afterEpochMillis +
                                ", period=" + period +
                                ", duration=" + duration +
                                ", zone=" + zone
                );
            }
            cur = cur.plus(period).plus(duration);
        }

        return cur.toInstant().toEpochMilli();
    }

    /**
     * Duration-only in O(1) using epochMillis.
     */
    private long nextDurationOnlyEpoch(Duration step, long afterEpochMillis) {
        if (step.isZero() || step.isNegative()) {
            throw new IllegalArgumentException("Duration step must be positive and non-zero.");
        }

        long stepMillis = step.toMillis();
        if (stepMillis <= 0) {
            throw new IllegalArgumentException(
                    "Duration resolution below 1ms is not supported with epochMillis API."
            );
        }

        long delta = afterEpochMillis - startEpochMillis;
        long k = delta / stepMillis + 1; // strictly AFTER
        return startEpochMillis + Math.multiplyExact(k, stepMillis);
    }

    /**
     * Safe fast-forward for period-only steps.
     */
    private OffsetDateTime fastForwardPeriodOnly(OffsetDateTime start, Period step, OffsetDateTime after) {
        OffsetDateTime cur = start;

        int stepMonths = step.getYears() * 12 + step.getMonths();
        int stepDays = step.getDays();

        if (stepMonths > 0) {
            long monthsBetween = ChronoUnit.MONTHS.between(cur, after);
            long k = monthsBetween / stepMonths;
            if (k > 0) {
                cur = cur.plusMonths(Math.multiplyExact(k, (long) stepMonths));
            }
            return cur;
        }

        if (stepMonths == 0 && stepDays > 0) {
            long daysBetween = ChronoUnit.DAYS.between(cur, after);
            long k = daysBetween / stepDays;
            if (k > 0) {
                cur = cur.plusDays(Math.multiplyExact(k, (long) stepDays));
            }
            return cur;
        }

        return cur;
    }
}
