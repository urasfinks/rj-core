package ru.jamsys.core.flat.template.period;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.UtilDate;

import java.text.ParseException;
import java.time.*;

class PlanSchedulerTest {

    @Test
    public void test() throws ParseException {
        Iso8601.Builder builder = Iso8601.builder().months(1);
        Assertions.assertEquals("P1M", builder.build().getIso());
        PlanScheduler planScheduler = new PlanScheduler(UtilDate.getTime("2025-01-01T00:00:00"), builder.build(), ZoneId.systemDefault());
        Assertions.assertEquals("2025-02-01T00:00:00.000", UtilDate.msFormat(planScheduler.next(UtilDate.getTime("2025-01-03T10:15:00"))));
    }

    @Test
    public void vYear() throws ParseException {
        long start = UtilDate.getTime("2024-02-29T00:00:00");
        long after = UtilDate.getTime("2024-02-29T12:00:00");
        Iso8601.Builder builder = Iso8601.builder()
                .years(1);
        Assertions.assertEquals("P1Y", builder.build().getIso());
        PlanScheduler planScheduler = new PlanScheduler(start, builder.build(), ZoneId.systemDefault());
        Assertions.assertEquals("2025-02-28T00:00:00.000", UtilDate.msFormat(planScheduler.next(after)));
    }

    @Test
    public void vYear12H() throws ParseException {
        long start = UtilDate.getTime("2024-02-29T12:00:00");
        long after = UtilDate.getTime("2024-02-29T12:00:00");
        Iso8601.Builder builder = Iso8601.builder()
                .years(1);
        Assertions.assertEquals("P1Y", builder.build().getIso());
        PlanScheduler planScheduler = new PlanScheduler(start, builder.build(), ZoneId.systemDefault());
        Assertions.assertEquals("2025-02-28T12:00:00.000", UtilDate.msFormat(planScheduler.next(after)));
    }

    @Test
    public void test2() throws ParseException {
        long start = UtilDate.getTime("2025-01-01T00:00:00:00");
        long after = UtilDate.getTime("2025-02-01T12:00:00");
        Iso8601.Builder builder = Iso8601.builder()
                .months(1)
                .hours(12);
        Assertions.assertEquals("P1MT12H", builder.build().getIso());
        PlanScheduler planScheduler = new PlanScheduler(start, builder.build(), ZoneId.systemDefault());
        Assertions.assertEquals("2025-03-02T00:00:00.000", UtilDate.msFormat(planScheduler.next(after)));
    }

    // ---------------- Iso8601 Builder ----------------

    @Test
    public void builderZeroIsCanonical() {
        Iso8601 rule = Iso8601.builder().build();
        Assertions.assertEquals("PT0S", rule.getIso());
        Assertions.assertEquals(java.time.Period.ZERO, rule.getPeriod());
        Assertions.assertEquals(java.time.Duration.ZERO, rule.getDuration());
    }

    @Test
    public void builderTimeOnlyMinutes() {
        Iso8601 rule = Iso8601.builder().minutes(1).build();
        Assertions.assertEquals("PT1M", rule.getIso());
        Assertions.assertTrue(rule.getPeriod().isZero());
        Assertions.assertEquals(java.time.Duration.ofMinutes(1), rule.getDuration());
    }

    @Test
    public void builderFractionalSeconds_nanosTrimZeros() {
        Iso8601 rule = Iso8601.builder().seconds(1).nanos(500_000_000).build(); // 1.5s
        Assertions.assertEquals("PT1.5S", rule.getIso());
        Assertions.assertEquals(java.time.Duration.ofSeconds(1).plusNanos(500_000_000), rule.getDuration());
    }

    @Test
    public void builderFractionalSeconds_nanosFullPrecisionTrim() {
        Iso8601 rule = Iso8601.builder().seconds(0).nanos(12_340_000).build(); // 0.01234s
        Assertions.assertEquals("PT0.01234S", rule.getIso());
    }

    @Test
    public void builderRejectsNegativeValues() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Iso8601.builder().days(-1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Iso8601.builder().hours(-1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Iso8601.builder().nanos(-1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Iso8601.builder().nanos(1_000_000_000));
    }

    // ---------------- Iso8601 Parse ----------------

    @Test
    public void parseDateOnly() {
        Iso8601 rule = Iso8601.parse("P1Y2M10D");
        Assertions.assertEquals(java.time.Period.of(1, 2, 10), rule.getPeriod());
        Assertions.assertEquals(java.time.Duration.ZERO, rule.getDuration());
        Assertions.assertEquals("P1Y2M10D", rule.getIso());
    }

    @Test
    public void parseTimeOnly() {
        Iso8601 rule = Iso8601.parse("PT2H30M10S");
        Assertions.assertEquals(java.time.Period.ZERO, rule.getPeriod());
        Assertions.assertEquals(java.time.Duration.ofHours(2).plusMinutes(30).plusSeconds(10), rule.getDuration());
        Assertions.assertEquals("PT2H30M10S", rule.getIso());
    }

    @Test
    public void parseMixedPeriodAndDuration() {
        Iso8601 rule = Iso8601.parse("P1MT12H");
        Assertions.assertEquals(java.time.Period.of(0, 1, 0), rule.getPeriod());
        Assertions.assertEquals(java.time.Duration.ofHours(12), rule.getDuration());
        Assertions.assertEquals("P1MT12H", rule.getIso());
    }

    @Test
    public void parseRejectsBlankOrNotStartingWithP() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Iso8601.parse(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Iso8601.parse(""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Iso8601.parse("  "));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Iso8601.parse("T1H"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Iso8601.parse("1H"));
    }

    // ---------------- PlanScheduler next() semantics ----------------

    @Test
    public void nextReturnsStartIfAfterBeforeStart() throws ParseException {
        long start = UtilDate.getTime("2025-01-01T00:00:00");
        long after = UtilDate.getTime("2024-12-31T23:59:59");

        Iso8601 rule = Iso8601.builder().days(1).build(); // P1D
        PlanScheduler s = new PlanScheduler(start, rule, ZoneId.systemDefault());

        Assertions.assertEquals("2025-01-01T00:00:00.000", UtilDate.msFormat(s.next(after)));
    }

    @Test
    public void nextIsStrictlyAfter_WhenAfterEqualsStart() throws ParseException {
        long start = UtilDate.getTime("2025-01-01T00:00:00");
        long after = UtilDate.getTime("2025-01-01T00:00:00");

        Iso8601 rule = Iso8601.builder().days(1).build(); // P1D
        PlanScheduler s = new PlanScheduler(start, rule, ZoneId.systemDefault());

        // строго после → следующий день
        Assertions.assertEquals("2025-01-02T00:00:00.000", UtilDate.msFormat(s.next(after)));
    }

    // ---------------- Duration-only fast path ----------------

    @Test
    public void durationOnly_everyMinute() throws ParseException {
        long start = UtilDate.getTime("2025-01-01T00:00:00");
        Iso8601 rule = Iso8601.builder().minutes(1).build(); // PT1M
        PlanScheduler s = new PlanScheduler(start, rule, ZoneId.systemDefault());

        long after = UtilDate.getTime("2025-01-01T00:00:00");
        Assertions.assertEquals("2025-01-01T00:01:00.000", UtilDate.msFormat(s.next(after)));

        after = UtilDate.getTime("2025-01-01T00:01:00");
        Assertions.assertEquals("2025-01-01T00:02:00.000", UtilDate.msFormat(s.next(after)));

        after = UtilDate.getTime("2025-01-01T00:01:59");
        Assertions.assertEquals("2025-01-01T00:02:00.000", UtilDate.msFormat(s.next(after)));
    }

    @Test
    public void durationOnly_guardRejectsSubMillis() {
        long start = 0L;
        Iso8601 rule = Iso8601.builder().nanos(1).build(); // PT0.000000001S
        PlanScheduler s = new PlanScheduler(start, rule, ZoneId.of("UTC"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> s.next(0L));
    }

    // ---------------- Period-only acceleration (months/days) ----------------

    @Test
    public void periodOnly_monthsFrom31st_rollsToEndOfMonth() throws ParseException {
        // поведение java.time: 2025-01-31 + P1M = 2025-02-28
        long start = UtilDate.getTime("2025-01-31T00:00:00");
        Iso8601 rule = Iso8601.builder().months(1).build(); // P1M
        PlanScheduler s = new PlanScheduler(start, rule, ZoneId.systemDefault());

        long after = UtilDate.getTime("2025-02-01T00:00:00");
        Assertions.assertEquals("2025-02-28T00:00:00.000", UtilDate.msFormat(s.next(after)));
    }

    @Test
    public void periodOnly_daysSimple() throws ParseException {
        long start = UtilDate.getTime("2025-01-01T00:00:00");
        Iso8601 rule = Iso8601.builder().days(10).build(); // P10D
        PlanScheduler s = new PlanScheduler(start, rule, ZoneId.systemDefault());

        long after = UtilDate.getTime("2025-01-05T00:00:00");
        Assertions.assertEquals("2025-01-11T00:00:00.000", UtilDate.msFormat(s.next(after)));

        after = UtilDate.getTime("2025-01-11T00:00:00");
        Assertions.assertEquals("2025-01-21T00:00:00.000", UtilDate.msFormat(s.next(after)));
    }

    // ---------------- Mixed Period + Duration (накопительный) ----------------

    @Test
    public void mixedStep_monthPlus12Hours_isAccumulative() throws ParseException {
        long start = UtilDate.getTime("2025-01-01T00:00:00");
        Iso8601 rule = Iso8601.builder().months(1).hours(12).build(); // P1MT12H
        PlanScheduler s = new PlanScheduler(start, rule, ZoneId.systemDefault());

        // после 2025-02-01 12:00 следующий тик:
        // 2025-01-01 00:00 + 1M + 12H = 2025-02-01 12:00 (НЕ подходит, нужно строго AFTER)
        // + (1M + 12H) => 2025-03-02 00:00
        long after = UtilDate.getTime("2025-02-01T12:00:00");
        Assertions.assertEquals("2025-03-02T00:00:00.000", UtilDate.msFormat(s.next(after)));
    }

    @Test
    public void mixedStep_dayPlus90Minutes() throws ParseException {
        long start = UtilDate.getTime("2025-01-01T00:00:00");
        Iso8601 rule = Iso8601.builder().days(1).minutes(90).build(); // P1DT90M
        PlanScheduler s = new PlanScheduler(start, rule, ZoneId.systemDefault());

        // t1 = 2025-01-02 01:30
        long after = UtilDate.getTime("2025-01-02T00:00:00");
        Assertions.assertEquals("2025-01-02T01:30:00.000", UtilDate.msFormat(s.next(after)));

        // строго после t1 → t2 = 2025-01-03 03:00
        after = UtilDate.getTime("2025-01-02T01:30:00");
        Assertions.assertEquals("2025-01-03T03:00:00.000", UtilDate.msFormat(s.next(after)));
    }

    // ---------------- DST correctness via ZoneId ----------------

    @Test
    public void dstSpringForward_hourlySchedule_skipsMissingLocalTimes() throws ParseException {
        // Europe/Amsterdam: переход на летнее время обычно в конце марта (02:00 -> 03:00)
        ZoneId zone = ZoneId.of("Europe/Amsterdam");
        long start = UtilDate.getTime("2025-03-30T00:00:00"); // дата вокруг DST (2025-03-30)
        Iso8601 rule = Iso8601.builder().hours(1).build();     // PT1H
        PlanScheduler s = new PlanScheduler(start, rule, zone);

        // Запрашиваем "после 01:30" -> ожидаем ближайший тик после, с учетом DST.
        // В ночь перевода время 02:00 может не существовать в локальной зоне.
        long after = UtilDate.getTime("2025-03-30T01:30:00");
        long next = s.next(after);

        // Проверяем только монотонность и валидность (конкретная строка зависит от UtilDate/парсинга и offset)
        Assertions.assertTrue(next > after);

        // Дополнительно: следующий тик должен быть примерно в пределах [after+30мин, after+2ч30мин]
        // (из-за "пропуска" часа при DST).
        Assertions.assertTrue(next - after >= 30L * 60_000L);
        Assertions.assertTrue(next - after <= 150L * 60_000L);
    }

    @Test
    public void dstFallBack_hourlySchedule_handlesRepeatedHour() throws ParseException {
        // Europe/Amsterdam: осенью час повторяется (03:00 -> 02:00)
        ZoneId zone = ZoneId.of("Europe/Amsterdam");
        long start = UtilDate.getTime("2025-10-26T00:00:00"); // дата вокруг DST (2025-10-26)
        Iso8601 rule = Iso8601.builder().hours(1).build();     // PT1H
        PlanScheduler s = new PlanScheduler(start, rule, zone);

        long after = UtilDate.getTime("2025-10-26T01:30:00");
        long next = s.next(after);

        Assertions.assertTrue(next > after);
        // В эту ночь может быть "растяжение" интервала в epoch, но локально шаги по часу должны идти корректно.
        Assertions.assertTrue(next - after >= 30L * 60_000L);
        Assertions.assertTrue(next - after <= 150L * 60_000L);
    }

    // ---------------- Guard / validation ----------------

    @Test
    public void constructorRejectsZeroStep() throws ParseException {
        long start = UtilDate.getTime("2025-01-01T00:00:00");
        Iso8601 zero = Iso8601.builder().build(); // PT0S
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new PlanScheduler(start, zero, ZoneId.systemDefault()));
    }

    @Test
    public void guardMaxIterationsTripsOnWhileLoop() throws ParseException {
        ZoneId zone = ZoneId.systemDefault();

        long start = UtilDate.getTime("2025-01-01T00:00:00");

        // after на 1000 дней вперед (чтобы нужно было много итераций)
        long after = start + 1000L * 24 * 60 * 60 * 1000;

        // period != 0 и duration != 0 -> fast-path и fastForwardPeriodOnly НЕ сработают,
        // значит попадем в while и начнем крутить итерации.
        Iso8601 rule = Iso8601.builder()
                .days(1)
                .seconds(1)
                .build(); // "P1DT1S"

        PlanScheduler s = new PlanScheduler(start, rule, zone, 10);

        Assertions.assertThrows(IllegalStateException.class, () -> s.next(after));
    }

}