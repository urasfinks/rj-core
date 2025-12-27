package ru.jamsys.core.flat.template.period;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import java.time.Duration;
import java.time.Period;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class NextOccurrenceTest {

    @Test
    public void test() {
        OffsetDateTime start = OffsetDateTime.parse("2025-01-01T00:00:00Z");
        OffsetDateTime after = OffsetDateTime.parse("2025-01-03T10:15:00Z");
        IsoDuration.Builder builder = IsoDuration.builder().months(1);
        Assertions.assertEquals("P1M", builder.build().getIso());
        Assertions.assertEquals("2025-02-01T00:00Z", NextOccurrence.next(start, builder.build(), after).toString());
    }

    @Test
    public void vYear() {
        OffsetDateTime start = OffsetDateTime.parse("2024-02-29T00:00:00Z");
        OffsetDateTime after = OffsetDateTime.parse("2024-02-29T12:00:00Z");
        IsoDuration.Builder builder = IsoDuration.builder()
                .years(1);
        Assertions.assertEquals("P1Y", builder.build().getIso());
        OffsetDateTime next = NextOccurrence.next(start, builder.build(), after);
        Assertions.assertEquals("2025-02-28T00:00Z", next.toString());
        //Assertions.assertEquals("2025-03-01T12:00Z", NextOccurrence.next(start, builder.build(), next));
    }

    @Test
    public void vYear12H() {
        OffsetDateTime start = OffsetDateTime.parse("2024-02-29T12:00:00Z");
        OffsetDateTime after = OffsetDateTime.parse("2024-02-29T12:00:00Z");
        IsoDuration.Builder builder = IsoDuration.builder()
                .years(1);
        Assertions.assertEquals("P1Y", builder.build().getIso());
        OffsetDateTime next = NextOccurrence.next(start, builder.build(), after);
        Assertions.assertEquals("2025-02-28T12:00Z", next.toString());
        //Assertions.assertEquals("2025-03-01T12:00Z", NextOccurrence.next(start, builder.build(), next));
    }

    @Test
    public void test2() {
        OffsetDateTime start = OffsetDateTime.parse("2025-01-01T00:00:00Z");
        OffsetDateTime after = OffsetDateTime.parse("2025-02-01T12:00Z");
        IsoDuration.Builder builder = IsoDuration.builder()
                .months(1)
                .hours(12);
        Assertions.assertEquals("P1MT12H", builder.build().getIso());
        OffsetDateTime next = NextOccurrence.next(start, builder.build(), after);
        // Важно понять, что если Duration период: 1 день 12 часов, то следующая дата будет эквивалентна + 36 часов,
        // а не 12 часов следующей даты
        Assertions.assertEquals("2025-03-02T00:00Z", next.toString());
        //Assertions.assertEquals("2025-03-01T12:00Z", NextOccurrence.next(start, builder.build(), next));
    }

    private static OffsetDateTime odt(int y, int m, int d, int hh, int mm, int ss) {
        return OffsetDateTime.of(y, m, d, hh, mm, ss, 0, ZoneOffset.UTC);
    }

    @Test
    void returnsStartIfAfterBeforeStart() {
        OffsetDateTime start = odt(2025, 1, 1, 0, 0, 0);
        OffsetDateTime after = odt(2024, 12, 31, 23, 59, 59);

        OffsetDateTime next = NextOccurrence.next(start, Period.ofDays(1), Duration.ZERO, after);
        assertEquals(start, next);
    }

    @Test
    void rejectsZeroStep() {
        OffsetDateTime start = odt(2025, 1, 1, 0, 0, 0);
        OffsetDateTime after = odt(2025, 1, 1, 0, 0, 0);

        assertThrows(IllegalArgumentException.class,
                () -> NextOccurrence.next(start, Period.ZERO, Duration.ZERO, after));
    }

    @Test
    void durationOnly_nextStrictlyAfter_afterOnTick() {
        OffsetDateTime start = odt(2025, 1, 1, 0, 0, 0);
        Duration step = Duration.ofMinutes(15);

        // ticks: 00:00, 00:15, 00:30, ...
        OffsetDateTime after = odt(2025, 1, 1, 0, 30, 0);

        OffsetDateTime next = NextOccurrence.next(start, Period.ZERO, step, after);
        assertEquals(odt(2025, 1, 1, 0, 45, 0), next);
    }

    @Test
    void durationOnly_nextStrictlyAfter_afterBetweenTicks() {
        OffsetDateTime start = odt(2025, 1, 1, 0, 0, 0);
        Duration step = Duration.ofHours(6);

        // ticks: 00:00, 06:00, 12:00, 18:00, ...
        OffsetDateTime after = odt(2025, 1, 1, 10, 0, 0);

        OffsetDateTime next = NextOccurrence.next(start, Period.ZERO, step, after);
        assertEquals(odt(2025, 1, 1, 12, 0, 0), next);
    }

    @Test
    void periodOnly_days_next() {
        OffsetDateTime start = odt(2025, 1, 1, 9, 0, 0);
        Period step = Period.ofDays(2);

        // ticks: Jan1 09:00, Jan3 09:00, Jan5 09:00, ...
        OffsetDateTime after = odt(2025, 1, 4, 0, 0, 0);

        OffsetDateTime next = NextOccurrence.next(start, step, Duration.ZERO, after);
        assertEquals(odt(2025, 1, 5, 9, 0, 0), next);
    }

    @Test
    void periodOnly_months_next_calendarLogic() {
        OffsetDateTime start = odt(2025, 1, 31, 10, 0, 0);
        Period step = Period.ofMonths(1);

        // ticks (Java Period month add):
        // 2025-01-31 10:00
        // 2025-02-28 10:00
        // 2025-03-28 10:00
        OffsetDateTime after = odt(2025, 2, 28, 10, 0, 0);

        OffsetDateTime next = NextOccurrence.next(start, step, Duration.ZERO, after);
        assertEquals(odt(2025, 3, 28, 10, 0, 0), next);
    }

    @Test
    void mixedStep_monthPlusHours_next() {
        OffsetDateTime start = odt(2025, 1, 1, 0, 0, 0);
        Period p = Period.ofMonths(1);
        Duration d = Duration.ofHours(12);

        // ticks:
        // 2025-01-01 00:00
        // 2025-02-01 12:00
        // 2025-03-01 00:00
        OffsetDateTime after = odt(2025, 2, 1, 12, 0, 0);

        OffsetDateTime next = NextOccurrence.next(start, p, d, after);
        assertEquals(odt(2025, 3, 2, 0, 0, 0), next);
    }

    @Test
    void isoStep_overload_works() {
        OffsetDateTime start = odt(2025, 1, 1, 0, 0, 0);
        OffsetDateTime after = odt(2025, 2, 1, 12, 0, 0);

        OffsetDateTime next = NextOccurrence.next(start, "P1MT12H", after);
        assertEquals(odt(2025, 3, 2, 0, 0, 0), next);
    }

    @Test
    void isoDuration_overload_works() {
        OffsetDateTime start = odt(2025, 1, 1, 0, 0, 0);
        OffsetDateTime after = odt(2025, 2, 1, 12, 0, 0);

        IsoDuration step = IsoDuration.parse("P1MT12H");
        OffsetDateTime next = NextOccurrence.next(start, step, after);

        assertEquals(odt(2025, 3, 2, 0, 0, 0), next);
    }

    @Test
    void rejectsNegativeSteps() {
        OffsetDateTime start = odt(2025, 1, 1, 0, 0, 0);
        OffsetDateTime after = odt(2025, 1, 2, 0, 0, 0);

        assertThrows(IllegalArgumentException.class,
                () -> NextOccurrence.next(start, Period.ofDays(-1), Duration.ZERO, after));

        assertThrows(IllegalArgumentException.class,
                () -> NextOccurrence.next(start, Period.ZERO, Duration.ofSeconds(-1), after));
    }

}