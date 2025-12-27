package ru.jamsys.core.flat.template.scheduler;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Fast "next execution time" calculator using carry-algorithm (no cartesian products).
 *
 * Semantics (as you specified):
 * - EMPTY LIST ([]) means "ANY" for that field.
 * - Non-empty list means "only these values allowed".
 *
 * Notes:
 * - days (day-of-month) + daysOfWeek are combined with AND when both are constrained.
 *   If you want OR semantics, see adjustDay() marked section.
 * - exclude: List<Integer> is treated as epoch seconds (int cannot hold epoch millis).
 * - offset: interpreted as seconds.
 * - truncateTime=true => truncate candidate to start-of-day before search.
 */
public final class TimeSchedulerPlan {

    private final TimeSchedulerRule rule;
    private final ZoneId zone;

    // Allowed sets; indexes are "natural values" (e.g., month 1..12, dow 1..7)
    private final BitSet secAllowed;   // 0..59
    private final BitSet minAllowed;   // 0..59
    private final BitSet hourAllowed;  // 0..23
    private final BitSet domAllowed;   // 1..31
    private final BitSet monthAllowed; // 1..12
    private final BitSet dowAllowed;   // 1..7

    private final NavigableSet<Integer> yearAllowedOrNull; // null => any year

    private final boolean truncateToDay;
    private final int offsetSeconds;

    private final Set<Long> excludeEpochMillis; // epoch millis

    // guard for unsatisfiable / bug cases
    private final int guardMaxIterations;

    public TimeSchedulerPlan(TimeSchedulerRule rule) {
        this(rule, ZoneId.systemDefault());
    }

    public TimeSchedulerPlan(TimeSchedulerRule rule, ZoneId zone) {
        this(rule, zone, 1_000_000);
    }

    public TimeSchedulerPlan(TimeSchedulerRule rule, ZoneId zone, int guardMaxIterations) {
        this.rule = Objects.requireNonNull(rule, "rule");
        this.zone = (zone == null) ? ZoneId.systemDefault() : zone;
        this.guardMaxIterations = Math.max(10_000, guardMaxIterations);

        this.truncateToDay = Boolean.TRUE.equals(rule.getTruncateTime());
        this.offsetSeconds = (rule.getOffset() == null) ? 0 : rule.getOffset();

        // Build allowed sets; empty list => ANY (full range)
        this.secAllowed = buildAllowed(rule.getSeconds(), 0, 59);
        this.minAllowed = buildAllowed(rule.getMinutes(), 0, 59);
        this.hourAllowed = buildAllowed(rule.getHours(), 0, 23);
        this.domAllowed = buildAllowed(rule.getDays(), 1, 31);
        this.dowAllowed = buildAllowed(rule.getDaysOfWeek(), 1, 7);

        this.monthAllowed = buildMonthAllowed(rule.getMonths(), rule.getQuarters());

        List<Integer> years = rule.getYears();
        if (years == null || years.isEmpty()) {
            this.yearAllowedOrNull = null; // ANY
        } else {
            // Sort + dedupe
            this.yearAllowedOrNull = new TreeSet<>(years);
        }

        // exclude: interpret as epoch seconds -> millis
        List<Integer> ex = rule.getExclude();
        if (ex == null || ex.isEmpty()) {
            this.excludeEpochMillis = Set.of();
        } else {
            this.excludeEpochMillis = ex.stream()
                    .filter(Objects::nonNull)
                    .mapToLong(i -> i.longValue() * 1000L)
                    .boxed()
                    .collect(Collectors.toSet());
        }
    }

    /**
     * Returns the next execution time strictly AFTER the provided timestamp.
     *
     * @param afterEpochMillis current time in epoch millis (exclusive boundary)
     * @return next execution time in epoch millis
     */
    public long nextAfter(long afterEpochMillis) {
        // Enforce rule.start as lower bound
        long base = Math.max(afterEpochMillis, rule.getStart());

        // Work in seconds granularity (your rule has seconds field)
        // "strictly after" => +1 second.
        long candidateMs = base + 1000L;

        // Apply phase offset (do search in shifted time domain)
        candidateMs -= offsetSeconds * 1000L;

        ZonedDateTime zdt = Instant.ofEpochMilli(candidateMs).atZone(zone);

        if (truncateToDay) {
            zdt = zdt.toLocalDate().atStartOfDay(zone);
        }

        for (int guard = 0; guard < guardMaxIterations; guard++) {
            ZonedDateTime matched = adjustToNextMatch(zdt);

            long outMs = matched.toInstant().toEpochMilli() + offsetSeconds * 1000L;

            // Must still satisfy start bound after offset is applied
            if (outMs <= afterEpochMillis) {
                // This can happen around truncation/offset edges; push forward.
                zdt = matched.plusSeconds(1);
                continue;
            }
            if (outMs < rule.getStart()) {
                // push to start boundary (in shifted domain)
                long ms = rule.getStart() - offsetSeconds * 1000L;
                zdt = Instant.ofEpochMilli(ms).atZone(zone);
                continue;
            }
            if (!excludeEpochMillis.contains(outMs)) {
                return outMs;
            }

            // Excluded => continue search strictly after this one
            zdt = matched.plusSeconds(1);
        }

        throw new IllegalStateException("No next time found (guard exceeded). Rule may be unsatisfiable.");
    }

    /**
     * Convenience: generate a series of next timestamps.
     */
    public List<Long> series(long afterEpochMillis, int count) {
        ArrayList<Long> result = new ArrayList<>(Math.max(0, count));
        long t = afterEpochMillis;
        for (int i = 0; i < count; i++) {
            long next = nextAfter(t);
            result.add(next);
            t = next;
        }
        return result;
    }

    // =========================
    // Core carry algorithm
    // =========================

    private ZonedDateTime adjustToNextMatch(ZonedDateTime zdt) {
        // Normalize nanos
        zdt = zdt.withNano(0);

        while (true) {
            // YEAR
            if (yearAllowedOrNull != null) {
                int y = zdt.getYear();
                Integer y2 = yearAllowedOrNull.ceiling(y);
                if (y2 == null) {
                    // no next allowed year -> unsatisfiable beyond this point
                    throw new IllegalStateException("No valid year >= " + y);
                }
                if (y2 != y) {
                    zdt = resetToYear(zdt, y2);
                    continue;
                }
            }

            // MONTH (1..12)
            int m = zdt.getMonthValue();
            int m2 = nextOrCarry(monthAllowed, m, 12);
            if (m2 == -1) {
                zdt = resetToYear(zdt.plusYears(1), zdt.getYear() + 1);
                continue;
            }
            if (m2 != m) {
                zdt = zdt.withMonth(m2).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
                continue;
            }

            // DAY (day-of-month + day-of-week)
            ZonedDateTime dayAdjusted = adjustDay(zdt);
            if (!dayAdjusted.equals(zdt)) {
                zdt = dayAdjusted;
                continue;
            }

            // HOUR (0..23)
            int h = zdt.getHour();
            int h2 = nextOrCarry(hourAllowed, h, 23);
            if (h2 == -1) {
                zdt = zdt.plusDays(1).truncatedTo(ChronoUnit.DAYS);
                continue;
            }
            if (h2 != h) {
                zdt = zdt.withHour(h2).withMinute(0).withSecond(0).withNano(0);
                continue;
            }

            // MINUTE (0..59)
            int mi = zdt.getMinute();
            int mi2 = nextOrCarry(minAllowed, mi, 59);
            if (mi2 == -1) {
                zdt = zdt.plusHours(1).withMinute(0).withSecond(0).withNano(0);
                continue;
            }
            if (mi2 != mi) {
                zdt = zdt.withMinute(mi2).withSecond(0).withNano(0);
                continue;
            }

            // SECOND (0..59)
            int s = zdt.getSecond();
            int s2 = nextOrCarry(secAllowed, s, 59);
            if (s2 == -1) {
                zdt = zdt.plusMinutes(1).withSecond(0).withNano(0);
                continue;
            }
            if (s2 != s) {
                zdt = zdt.withSecond(s2).withNano(0);
                continue;
            }

            // Final defensive check (useful if you tweak adjustDay semantics)
            if (matchesAll(zdt)) {
                return zdt.withNano(0);
            }

            // If mismatch somehow remained, advance by 1 second and retry
            zdt = zdt.plusSeconds(1);
        }
    }

    /**
     * Adjusts date part (Y-M-D) to satisfy:
     * - monthAllowed (already satisfied when called)
     * - domAllowed (if constrained)
     * - dowAllowed (if constrained)
     *
     * Current implementation: AND semantics when both constrained.
     * If you want OR semantics, see comment below.
     */
    private ZonedDateTime adjustDay(ZonedDateTime zdt) {
        boolean domAny = isFull(domAllowed, 1, 31);
        boolean dowAny = isFull(dowAllowed, 1, 7);

        if (domAny && dowAny) {
            return zdt;
        }

        LocalDate baseDate = zdt.toLocalDate();

        // Search forward day-by-day, but with a tight bound.
        // In practice this is fast because it runs only when day constraints exist.
        for (int step = 0; step < 400; step++) {
            LocalDate d = baseDate.plusDays(step);

            // month constraint should already hold, but crossing months can happen due to carries
            if (!monthAllowed.get(d.getMonthValue())) continue;

            int dom = d.getDayOfMonth();
            int dow = d.getDayOfWeek().getValue(); // 1..7

            boolean domOk = domAny || domAllowed.get(dom);
            boolean dowOk = dowAny || dowAllowed.get(dow);

            // AND semantics:
            boolean ok = domOk && dowOk;

            // If you want OR semantics (cron-like), replace with:
            // boolean ok = (domAny ? dowOk : (dowAny ? domOk : (domOk || dowOk)));

            if (ok) {
                if (step == 0) {
                    return zdt;
                }
                return d.atStartOfDay(zone);
            }
        }

        // If not found within ~13 months, push to next allowed month start and let upper loop handle carries.
        ZonedDateTime nextMonth = zdt.plusMonths(1).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        return nextMonth;
    }

    private boolean matchesAll(ZonedDateTime zdt) {
        if (yearAllowedOrNull != null && !yearAllowedOrNull.contains(zdt.getYear())) return false;
        if (!monthAllowed.get(zdt.getMonthValue())) return false;

        int dom = zdt.getDayOfMonth();
        int dow = zdt.getDayOfWeek().getValue();
        boolean domAny = isFull(domAllowed, 1, 31);
        boolean dowAny = isFull(dowAllowed, 1, 7);

        boolean domOk = domAny || domAllowed.get(dom);
        boolean dowOk = dowAny || dowAllowed.get(dow);

        // Must mirror adjustDay() semantics:
        if (!(domOk && dowOk)) return false;

        if (!hourAllowed.get(zdt.getHour())) return false;
        if (!minAllowed.get(zdt.getMinute())) return false;
        return secAllowed.get(zdt.getSecond());
    }

    private static ZonedDateTime resetToYear(ZonedDateTime zdt, int year) {
        return zdt.withYear(year)
                .withMonth(1)
                .withDayOfMonth(1)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
    }

    // =========================
    // Builders / helpers
    // =========================

    /**
     * Build allowed BitSet for [min..max] inclusive.
     * Empty list or null => FULL range (ANY).
     */
    private static BitSet buildAllowed(List<Integer> values, int min, int max) {
        BitSet bs = new BitSet(max + 1);
        if (values == null || values.isEmpty()) {
            for (int i = min; i <= max; i++) bs.set(i);
            return bs;
        }
        for (Integer v : values) {
            if (v == null) continue;
            if (v < min || v > max) {
                throw new IllegalArgumentException("value out of range [" + min + ".." + max + "]: " + v);
            }
            bs.set(v);
        }
        if (bs.isEmpty()) {
            throw new IllegalArgumentException("allowed set is empty for range [" + min + ".." + max + "]");
        }
        return bs;
    }

    /**
     * months + quarters handling for "[] means ANY".
     * - both empty => all months
     * - months only => months
     * - quarters only => quarter months
     * - both => intersection
     */
    private static BitSet buildMonthAllowed(List<Integer> months, List<Integer> quarters) {
        boolean monthsAny = (months == null || months.isEmpty());
        boolean quartersAny = (quarters == null || quarters.isEmpty());

        if (monthsAny && quartersAny) {
            return fullRangeBitSet(1, 12);
        }

        if (!monthsAny && quartersAny) {
            return buildAllowed(months, 1, 12);
        }

        BitSet quarterMonths = new BitSet(13);
        if (quartersAny) {
            // not reachable due to earlier branches, but keep safe
            for (int m = 1; m <= 12; m++) quarterMonths.set(m);
        } else {
            for (Integer q : quarters) {
                if (q == null) continue;
                if (q < 1 || q > 4) {
                    throw new IllegalArgumentException("quarter out of range [1..4]: " + q);
                }
                int start = (q - 1) * 3 + 1;
                for (int m = start; m < start + 3; m++) quarterMonths.set(m);
            }
        }

        if (monthsAny) {
            if (quarterMonths.isEmpty()) {
                throw new IllegalArgumentException("quarters produced empty month set");
            }
            return quarterMonths;
        }

        BitSet monthSet = buildAllowed(months, 1, 12);
        monthSet.and(quarterMonths);
        if (monthSet.isEmpty()) {
            throw new IllegalArgumentException("months/quarters intersection is empty");
        }
        return monthSet;
    }

    private static BitSet fullRangeBitSet(int min, int max) {
        BitSet bs = new BitSet(max + 1);
        for (int i = min; i <= max; i++) bs.set(i);
        return bs;
    }

    /**
     * Returns:
     * - current if current is allowed
     * - next allowed >= current if exists
     * - -1 if no allowed value in [current..max] (carry)
     */
    private static int nextOrCarry(BitSet allowed, int current, int max) {
        int next = allowed.nextSetBit(current);
        if (next == current) return current;
        if (next != -1 && next <= max) return next;
        return -1;
    }

    private static boolean isFull(BitSet bs, int min, int max) {
        int first = bs.nextSetBit(min);
        if (first != min) return false;
        for (int i = min; i <= max; i++) {
            if (!bs.get(i)) return false;
        }
        return true;
    }
}
