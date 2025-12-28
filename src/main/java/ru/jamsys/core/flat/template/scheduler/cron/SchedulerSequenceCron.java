package ru.jamsys.core.flat.template.scheduler.cron;

import ru.jamsys.core.flat.template.scheduler.SchedulerSequence;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public final class SchedulerSequenceCron implements SchedulerSequence {

    private final ZoneId zone;

    // Allowed sets; indexes are "natural values" (e.g., month 1..12, dow 1..7)
    private final BitSet secAllowed;   // 0..59
    private final BitSet minAllowed;   // 0..59
    private final BitSet hourAllowed;  // 0..23
    private final BitSet domAllowed;   // 1..31
    private final BitSet monthAllowed; // 1..12
    private final BitSet dowAllowed;   // 1..7

    private final NavigableSet<Integer> yearAllowedOrNull; // null => any year

    private final Set<Long> excludeEpochMillis; // epoch millis

    // guard for unsatisfiable / bug cases
    private final int guardMaxIterations;

    public SchedulerSequenceCron(SchedulerTemplateCron schedulerTemplateCron) {
        this(schedulerTemplateCron, ZoneId.systemDefault());
    }

    public SchedulerSequenceCron(SchedulerTemplateCron rule, ZoneId zone) {
        this(rule, zone, 1_000_000);
    }

    public SchedulerSequenceCron(SchedulerTemplateCron template, ZoneId zone, int guardMaxIterations) {
        super();
        this.zone = (zone == null) ? ZoneId.systemDefault() : zone;
        this.guardMaxIterations = Math.max(10_000, guardMaxIterations);

        // Build allowed sets; empty list => ANY (full range)
        this.secAllowed = buildAllowed(template.getSeconds(), 0, 59);
        this.minAllowed = buildAllowed(template.getMinutes(), 0, 59);
        this.hourAllowed = buildAllowed(template.getHours(), 0, 23);
        this.domAllowed = buildAllowed(template.getDays(), 1, 31);
        this.dowAllowed = buildAllowed(template.getDaysOfWeek(), 1, 7);

        this.monthAllowed = buildMonthAllowed(template.getMonths(), template.getQuarters());

        List<Integer> years = template.getYears();
        if (years == null || years.isEmpty()) {
            this.yearAllowedOrNull = null; // ANY
        } else {
            // Sort + dedupe
            this.yearAllowedOrNull = new TreeSet<>(years);
        }

        // exclude: interpret as epoch seconds -> millis
        List<Integer> ex = template.getExclude();
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

    public long next(long afterEpochMillis) {

        // Work in seconds granularity (your rule has seconds field)
        // "strictly after" => +1 second.

        ZonedDateTime zdt = Instant.ofEpochMilli(afterEpochMillis).atZone(zone);

        for (int guard = 0; guard < guardMaxIterations; guard++) {
            ZonedDateTime matched = adjustToNextMatch(zdt);

            long outMs = matched.toInstant().toEpochMilli();

            if (outMs <= afterEpochMillis) {
                zdt = matched.plusSeconds(1);
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

    public List<Long> series(long afterEpochMillis, int count) {
        ArrayList<Long> result = new ArrayList<>(Math.max(0, count));
        long t = afterEpochMillis;
        for (int i = 0; i < count; i++) {
            long next = next(t);
            result.add(next);
            t = next;
        }
        return result;
    }

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
        return zdt.plusMonths(1).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
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

    private static BitSet buildMonthAllowed(List<Integer> months, List<Integer> quarters) {
        boolean monthsAny = (months == null || months.isEmpty());
        boolean quartersAny = (quarters == null || quarters.isEmpty());

        if (monthsAny && quartersAny) {
            return fullRangeBitSet(1, 12);
        }
        if (!monthsAny && quartersAny) {
            return buildAllowed(months, 1, 12);
        }

        // quartersAny == false here (otherwise we would have returned above)
        BitSet quarterMonths = buildQuarterMonths(quarters);

        if (monthsAny) {
            return quarterMonths;
        }

        BitSet monthSet = buildAllowed(months, 1, 12);
        monthSet.and(quarterMonths);
        if (monthSet.isEmpty()) {
            throw new IllegalArgumentException("months/quarters intersection is empty");
        }
        return monthSet;
    }

    private static BitSet buildQuarterMonths(List<Integer> quarters) {
        if (quarters == null || quarters.isEmpty()) {
            // По смыслу buildMonthAllowed сюда не должен попадать,
            // но делаем безопасно:
            return fullRangeBitSet(1, 12);
        }

        BitSet quarterMonths = new BitSet(13);
        for (Integer q : quarters) {
            if (q == null) continue;
            if (q < 1 || q > 4) {
                throw new IllegalArgumentException("quarter out of range [1..4]: " + q);
            }
            int start = (q - 1) * 3 + 1; // 1,4,7,10
            quarterMonths.set(start);
            quarterMonths.set(start + 1);
            quarterMonths.set(start + 2);
        }

        if (quarterMonths.isEmpty()) {
            throw new IllegalArgumentException("quarters produced empty month set");
        }
        return quarterMonths;
    }

    @SuppressWarnings("all")
    private static BitSet fullRangeBitSet(int min, int max) {
        BitSet bs = new BitSet(max + 1);
        for (int i = min; i <= max; i++) bs.set(i);
        return bs;
    }

    private static int nextOrCarry(BitSet allowed, int current, int max) {
        int next = allowed.nextSetBit(current);
        if (next == current) return current;
        if (next != -1 && next <= max) return next;
        return -1;
    }

    @SuppressWarnings("all")
    private static boolean isFull(BitSet bs, int min, int max) {
        int first = bs.nextSetBit(min);
        if (first != min) return false;
        for (int i = min; i <= max; i++) {
            if (!bs.get(i)) return false;
        }
        return true;
    }

}
