package ru.jamsys.core.flat.template.scheduler.iso8601;

import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public final class SequenceISO8601 {

    private final TemplateISO8601 template;
    private int guardMaxIterations = 1_000_000;
    private OffsetDateTime currentOffset;

    public SequenceISO8601(TemplateISO8601 template) {
        Objects.requireNonNull(template, "template must not be null");

        this.template = template;
        this.currentOffset = OffsetDateTime.ofInstant(
                Instant.ofEpochMilli(template.getStartEpochMillis()),
                template.getZone()
        );
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

    public long next(long afterEpochMillis) {
        // If reference is before schedule start → return start
        if (afterEpochMillis < template.getStartEpochMillis()) {
            return template.getStartEpochMillis();
        }

        // Fast path: duration-only (pure epoch math, O(1))
        if (template.getPeriod().isZero()) {
            return nextDurationOnlyEpoch(template.getDuration(), afterEpochMillis);
        }

        OffsetDateTime after = OffsetDateTime.ofInstant(Instant.ofEpochMilli(afterEpochMillis), template.getZone());

        // Safe acceleration only for period-only rules
        if (template.getDuration().isZero()) {
            currentOffset = fastForwardPeriodOnly(currentOffset, template.getPeriod(), after);
        }

        int it = 0;
        while (!currentOffset.isAfter(after)) {
            if (++it > guardMaxIterations) {
                throw new ForwardException(
                        "guardMaxIterations exceeded",
                        new HashMapBuilder<String, Object>()
                                .append("guardMaxIterations", guardMaxIterations)
                                .append("template", template)
                );
            }
            currentOffset = currentOffset.plus(template.getPeriod()).plus(template.getDuration());
        }

        return currentOffset.toInstant().toEpochMilli();
    }

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

        long delta = afterEpochMillis - template.getStartEpochMillis();
        long k = delta / stepMillis + 1; // strictly AFTER
        return template.getStartEpochMillis() + Math.multiplyExact(k, stepMillis);
    }

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
