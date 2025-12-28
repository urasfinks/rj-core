package ru.jamsys.core.flat.template.scheduler.interval;

import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.flat.template.scheduler.SchedulerSequence;
import ru.jamsys.core.flat.util.Util;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class SchedulerIntervalSequence implements SchedulerSequence {

    private final SchedulerIntervalTemplate template;

    private int guardMaxIterations = 1_000_000;

    /** Cursor (state) in schedule timeline. Always moves forward. */
    private OffsetDateTime currentOffset;

    /** Enforces monotonic afterEpochMillis contract (forward-only usage). */
    private long lastAfterEpochMillis = Long.MIN_VALUE;

    public SchedulerIntervalSequence(SchedulerIntervalTemplate template) {
        this.template = Objects.requireNonNull(template, "template must not be null");

        this.currentOffset = OffsetDateTime.ofInstant(
                Instant.ofEpochMilli(template.getStartEpochMillis()),
                template.getZone()
        );

        validateStepOrThrow();
    }

    @SuppressWarnings("unused")
    public void setGuardMaxIterations(int guardMaxIterations) {
        if (guardMaxIterations <= 0) {
            throw new ForwardException(
                    "guardMaxIterations must be > 0",
                    new HashMapBuilder<String, Object>()
                            .append("guardMaxIterations", guardMaxIterations)
                            .append("template", template)
            );
        }
        this.guardMaxIterations = guardMaxIterations;
    }

    /**
     * Returns next occurrence strictly after {@code afterEpochMillis}.
     * Forward-only contract: {@code afterEpochMillis} must be non-decreasing between calls.
     */
    public long next(long afterEpochMillis) {
        enforceForwardOnly(afterEpochMillis);

        // Local cache (hot path): avoids repeating virtual calls; no extra fields in the object.
        final long startEpochMillis = template.getStartEpochMillis();
        final ZoneId zone = template.getZone();
        final Period period = template.getPeriod();
        final Duration duration = template.getDuration();

        if (afterEpochMillis < startEpochMillis) {
            // Keep cursor consistent with returned value
            if (currentOffset.toInstant().toEpochMilli() != startEpochMillis) {
                currentOffset = OffsetDateTime.ofInstant(Instant.ofEpochMilli(startEpochMillis), zone);
            }
            return startEpochMillis;
        }

        // Duration-only (Period=0): pure epoch math, O(1)
        if (period.isZero()) {
            return nextDurationOnlyEpoch(duration, startEpochMillis, afterEpochMillis);
        }

        final OffsetDateTime after = OffsetDateTime.ofInstant(
                Instant.ofEpochMilli(afterEpochMillis),
                zone
        );

        // Defensive: never allow cursor before start
        if (currentOffset.toInstant().toEpochMilli() < startEpochMillis) {
            currentOffset = OffsetDateTime.ofInstant(Instant.ofEpochMilli(startEpochMillis), zone);
        }

        // Mixed Period+Duration: general jump is unsafe, loop with guard.
        if (!duration.isZero()) {
            return advanceUntilAfter(after, period, duration);
        }

        // Period-only: safe fast-forward for pure months or pure days.
        currentOffset = fastForwardPeriodOnly(currentOffset, after, period);

        // Final strictness step (usually 0..few iters)
        return advanceUntilAfter(after, period, Duration.ZERO);
    }

    // ---------------------- helpers ----------------------

    private void validateStepOrThrow() {
        final Period p = template.getPeriod();
        final Duration d = template.getDuration();

        if (p.isNegative()) {
            throw new ForwardException(
                    "Period step must be non-negative for forward-only iteration",
                    new HashMapBuilder<String, Object>()
                            .append("period", p)
                            .append("template", template)
            );
        }
        if (d.isNegative()) {
            throw new ForwardException(
                    "Duration step must be non-negative for forward-only iteration",
                    new HashMapBuilder<String, Object>()
                            .append("duration", d)
                            .append("template", template)
            );
        }
        if (p.isZero() && d.isZero()) {
            throw new ForwardException(
                    "Schedule step must not be zero (period and duration are both zero)",
                    new HashMapBuilder<String, Object>()
                            .append("period", p)
                            .append("duration", d)
                            .append("template", template)
            );
        }
    }

    private void enforceForwardOnly(long afterEpochMillis) {
        if (afterEpochMillis < lastAfterEpochMillis) {
            throw new ForwardException(
                    "afterEpochMillis must be non-decreasing (forward-only SequenceISO8601)",
                    new HashMapBuilder<String, Object>()
                            .append("afterEpochMillis", afterEpochMillis)
                            .append("lastAfterEpochMillis", lastAfterEpochMillis)
                            .append("template", template)
            );
        }
        lastAfterEpochMillis = afterEpochMillis;
    }

    private long nextDurationOnlyEpoch(Duration step, long startEpochMillis, long afterEpochMillis) {
        if (step.isZero() || step.isNegative()) {
            throw new ForwardException(
                    "Duration step must be positive and non-zero",
                    new HashMapBuilder<String, Object>()
                            .append("template", template)
            );
        }

        final long stepMillis = step.toMillis();
        if (stepMillis <= 0) {
            throw new ForwardException(
                    "Duration resolution below 1ms is not supported with epochMillis API",
                    new HashMapBuilder<String, Object>()
                            .append("template", template)
            );
        }

        final long delta = afterEpochMillis - startEpochMillis;

        // strictly AFTER
        final long k = delta / stepMillis + 1;
        return startEpochMillis + Math.multiplyExact(k, stepMillis);
    }

    private long advanceUntilAfter(OffsetDateTime after, Period period, Duration duration) {
        int it = 0;
        final boolean hasPeriod = !period.isZero();
        final boolean hasDuration = !duration.isZero();

        while (!currentOffset.isAfter(after)) {
            if (++it > guardMaxIterations) {
                throw new ForwardException(
                        "guardMaxIterations exceeded",
                        new HashMapBuilder<String, Object>()
                                .append("guardMaxIterations", guardMaxIterations)
                                .append("template", template)
                                .append("currentOffset", Util.firstNonNull(currentOffset, "null").toString())
                                .append("after", Util.firstNonNull(after, "null").toString())
                );
            }

            if (hasPeriod && hasDuration) {
                currentOffset = currentOffset.plus(period).plus(duration);
            } else if (hasPeriod) {
                currentOffset = currentOffset.plus(period);
            } else {
                currentOffset = currentOffset.plus(duration);
            }
        }

        return currentOffset.toInstant().toEpochMilli();
    }

    /**
     * Safe fast-forward for period-only schedules:
     * - Pure month-based (years/months only)
     * - Pure day-based (days only)
     * Any mix of months+days returns input cursor without acceleration.
     */
    private OffsetDateTime fastForwardPeriodOnly(OffsetDateTime start, OffsetDateTime after, Period period) {
        final int stepMonths = period.getYears() * 12 + period.getMonths();
        final int stepDays = period.getDays();

        if (stepMonths == 0 && stepDays == 0) {
            return start; // already validated as "should not happen", but safe
        }
        if (stepMonths < 0 || stepDays < 0) {
            throw new ForwardException(
                    "Period step must be positive for forward-only iteration",
                    new HashMapBuilder<String, Object>()
                            .append("period", period)
                            .append("template", template)
            );
        }

        // Mixed months+days: conservative
        if (stepMonths != 0 && stepDays != 0) {
            return start;
        }

        if (stepMonths > 0) {
            long monthsBetween = ChronoUnit.MONTHS.between(start, after);
            if (monthsBetween > 0) {
                long k = monthsBetween / stepMonths;
                if (k > 0) {
                    start = start.plusMonths(Math.multiplyExact(k, (long) stepMonths));
                }
            }

            int tighten = 0;
            while (!start.plusMonths(stepMonths).isAfter(after)) {
                if (++tighten > 16) break;
                start = start.plusMonths(stepMonths);
            }
            return start;
        }

        // Pure day-based (stepDays > 0)
        long daysBetween = ChronoUnit.DAYS.between(start, after);
        if (daysBetween > 0) {
            long k = daysBetween / stepDays;
            if (k > 0) {
                start = start.plusDays(Math.multiplyExact(k, (long) stepDays));
            }
        }

        int tighten = 0;
        while (!start.plusDays(stepDays).isAfter(after)) {
            if (++tighten > 16) break;
            start = start.plusDays(stepDays);
        }
        return start;
    }
}
