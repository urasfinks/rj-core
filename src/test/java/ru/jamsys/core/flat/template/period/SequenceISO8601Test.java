package ru.jamsys.core.flat.template.period;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.flat.template.scheduler.iso8601.TemplateISO8601;
import ru.jamsys.core.flat.template.scheduler.iso8601.SequenceISO8601;
import ru.jamsys.core.flat.util.UtilDate;

import java.text.ParseException;
import java.time.*;

class SequenceISO8601Test {

    @Test
    public void test() throws ParseException {
        TemplateISO8601.Builder builder = TemplateISO8601.builder(
                        UtilDate.getTime("2025-01-01T00:00:00"),
                        ZoneId.systemDefault()
                )
                .months(1);
        Assertions.assertEquals("P1M", builder.build().getIso());
        SequenceISO8601 sequenceISO8601 = new SequenceISO8601(builder.build());
        Assertions.assertEquals(
                "2025-02-01T00:00:00.000",
                UtilDate.msFormat(sequenceISO8601.next(UtilDate.getTime("2025-01-03T10:15:00")))
        );
    }

    @Test
    public void vYear() throws ParseException {
        TemplateISO8601.Builder builder = TemplateISO8601.builder(
                        UtilDate.getTime("2024-02-29T00:00:00"),
                        ZoneId.systemDefault()
                )
                .years(1);
        Assertions.assertEquals("P1Y", builder.build().getIso());
        SequenceISO8601 sequenceISO8601 = new SequenceISO8601(builder.build());
        Assertions.assertEquals(
                "2025-02-28T00:00:00.000",
                UtilDate.msFormat(sequenceISO8601.next(UtilDate.getTime("2024-02-29T12:00:00")))
        );
    }

    @Test
    public void vYear12H() throws ParseException {
        TemplateISO8601.Builder builder = TemplateISO8601.builder(
                        UtilDate.getTime("2024-02-29T12:00:00"),
                        ZoneId.systemDefault()
                )
                .years(1);
        Assertions.assertEquals("P1Y", builder.build().getIso());
        SequenceISO8601 sequenceISO8601 = new SequenceISO8601(builder.build());
        Assertions.assertEquals(
                "2025-02-28T12:00:00.000",
                UtilDate.msFormat(sequenceISO8601.next(UtilDate.getTime("2024-02-29T12:00:00")))
        );
    }

    @Test
    public void test2() throws ParseException {
        TemplateISO8601.Builder builder = TemplateISO8601.builder(
                        UtilDate.getTime("2025-01-01T00:00:00:00"),
                        ZoneId.systemDefault()
                )
                .months(1)
                .hours(12);
        Assertions.assertEquals("P1MT12H", builder.build().getIso());
        SequenceISO8601 sequenceISO8601 = new SequenceISO8601(builder.build());
        Assertions.assertEquals(
                "2025-03-02T00:00:00.000",
                UtilDate.msFormat(sequenceISO8601.next(UtilDate.getTime("2025-02-01T12:00:00")))
        );
    }

    // ---------------- Iso8601 Builder ----------------

    @Test
    public void builderTimeOnlyMinutes() {
        TemplateISO8601 rule = TemplateISO8601.builder(0, ZoneId.systemDefault()).minutes(1).build();
        Assertions.assertEquals("PT1M", rule.getIso());
        Assertions.assertTrue(rule.getPeriod().isZero());
        Assertions.assertEquals(java.time.Duration.ofMinutes(1), rule.getDuration());
    }

    @Test
    public void builderFractionalSeconds_nanosTrimZeros() {
        TemplateISO8601 rule = TemplateISO8601.builder(0, ZoneId.systemDefault()).seconds(1).nanos(500_000_000).build(); // 1.5s
        Assertions.assertEquals("PT1.5S", rule.getIso());
        Assertions.assertEquals(java.time.Duration.ofSeconds(1).plusNanos(500_000_000), rule.getDuration());
    }

    @Test
    public void builderFractionalSeconds_nanosFullPrecisionTrim() {
        TemplateISO8601 rule = TemplateISO8601.builder(0, ZoneId.systemDefault()).seconds(0).nanos(12_340_000).build(); // 0.01234s
        Assertions.assertEquals("PT0.01234S", rule.getIso());
    }

    @Test
    public void builderRejectsNegativeValues() {
        Assertions.assertThrows(ForwardException.class, () -> TemplateISO8601.builder(0, ZoneId.systemDefault()).days(-1));
        Assertions.assertThrows(ForwardException.class, () -> TemplateISO8601.builder(0, ZoneId.systemDefault()).hours(-1));
        Assertions.assertThrows(ForwardException.class, () -> TemplateISO8601.builder(0, ZoneId.systemDefault()).nanos(-1));
        Assertions.assertThrows(ForwardException.class, () -> TemplateISO8601.builder(0, ZoneId.systemDefault()).nanos(1_000_000_000));
    }

    // ---------------- Iso8601 Parse ----------------

    @Test
    public void parseDateOnly() {
        TemplateISO8601 rule = TemplateISO8601.parse("P1Y2M10D", 0, ZoneId.systemDefault());
        Assertions.assertEquals(java.time.Period.of(1, 2, 10), rule.getPeriod());
        Assertions.assertEquals(java.time.Duration.ZERO, rule.getDuration());
        Assertions.assertEquals("P1Y2M10D", rule.getIso());
    }

    @Test
    public void parseTimeOnly() {
        TemplateISO8601 rule = TemplateISO8601.parse("PT2H30M10S", 0, ZoneId.systemDefault());
        Assertions.assertEquals(java.time.Period.ZERO, rule.getPeriod());
        Assertions.assertEquals(java.time.Duration.ofHours(2).plusMinutes(30).plusSeconds(10), rule.getDuration());
        Assertions.assertEquals("PT2H30M10S", rule.getIso());
    }

    @Test
    public void parseMixedPeriodAndDuration() {
        TemplateISO8601 rule = TemplateISO8601.parse("P1MT12H", 0, ZoneId.systemDefault());
        Assertions.assertEquals(java.time.Period.of(0, 1, 0), rule.getPeriod());
        Assertions.assertEquals(java.time.Duration.ofHours(12), rule.getDuration());
        Assertions.assertEquals("P1MT12H", rule.getIso());
    }

    @Test
    public void parseRejectsBlankOrNotStartingWithP() {
        Assertions.assertThrows(ForwardException.class, () -> TemplateISO8601.parse(null, 0, ZoneId.systemDefault()));
        Assertions.assertThrows(ForwardException.class, () -> TemplateISO8601.parse("", 0, ZoneId.systemDefault()));
        Assertions.assertThrows(ForwardException.class, () -> TemplateISO8601.parse("  ", 0, ZoneId.systemDefault()));
        Assertions.assertThrows(ForwardException.class, () -> TemplateISO8601.parse("T1H", 0, ZoneId.systemDefault()));
        Assertions.assertThrows(ForwardException.class, () -> TemplateISO8601.parse("1H", 0, ZoneId.systemDefault()));
    }

    // ---------------- PlanScheduler next() semantics ----------------

    @Test
    public void nextReturnsStartIfAfterBeforeStart() throws ParseException {
        TemplateISO8601 rule = TemplateISO8601.builder(UtilDate.getTime("2025-01-01T00:00:00"), ZoneId.systemDefault()).days(1).build(); // P1D
        SequenceISO8601 s = new SequenceISO8601(rule);

        Assertions.assertEquals("2025-01-01T00:00:00.000", UtilDate.msFormat(s.next(UtilDate.getTime("2024-12-31T23:59:59"))));
    }

    @Test
    public void nextIsStrictlyAfter_WhenAfterEqualsStart() throws ParseException {
        TemplateISO8601 rule = TemplateISO8601.builder(UtilDate.getTime("2025-01-01T00:00:00"), ZoneId.systemDefault()).days(1).build(); // P1D
        SequenceISO8601 s = new SequenceISO8601(rule);

        // строго после → следующий день
        Assertions.assertEquals("2025-01-02T00:00:00.000", UtilDate.msFormat(s.next(UtilDate.getTime("2025-01-01T00:00:00"))));
    }

    // ---------------- Duration-only fast path ----------------

    @Test
    public void durationOnly_everyMinute() throws ParseException {

        TemplateISO8601 rule = TemplateISO8601.builder(UtilDate.getTime("2025-01-01T00:00:00"), ZoneId.systemDefault()).minutes(1).build(); // PT1M
        SequenceISO8601 s = new SequenceISO8601(rule);

        long after = UtilDate.getTime("2025-01-01T00:00:00");
        Assertions.assertEquals("2025-01-01T00:01:00.000", UtilDate.msFormat(s.next(after)));

        after = UtilDate.getTime("2025-01-01T00:01:00");
        Assertions.assertEquals("2025-01-01T00:02:00.000", UtilDate.msFormat(s.next(after)));

        after = UtilDate.getTime("2025-01-01T00:01:59");
        Assertions.assertEquals("2025-01-01T00:02:00.000", UtilDate.msFormat(s.next(after)));
    }

    @Test
    public void durationOnly_guardRejectsSubMillis() {
        TemplateISO8601 rule = TemplateISO8601.builder(0, ZoneId.of("UTC")).nanos(1).build(); // PT0.000000001S
        SequenceISO8601 s = new SequenceISO8601(rule);
        Assertions.assertThrows(IllegalArgumentException.class, () -> s.next(0L));
    }

    // ---------------- Period-only acceleration (months/days) ----------------

    @Test
    public void periodOnly_monthsFrom31st_rollsToEndOfMonth() throws ParseException {
        // поведение java.time: 2025-01-31 + P1M = 2025-02-28

        TemplateISO8601 rule = TemplateISO8601.builder(UtilDate.getTime("2025-01-31T00:00:00"), ZoneId.systemDefault()).months(1).build(); // P1M
        SequenceISO8601 s = new SequenceISO8601(rule);

        long after = UtilDate.getTime("2025-02-01T00:00:00");
        Assertions.assertEquals("2025-02-28T00:00:00.000", UtilDate.msFormat(s.next(after)));
    }

    @Test
    public void periodOnly_daysSimple() throws ParseException {
        TemplateISO8601 rule = TemplateISO8601.builder(UtilDate.getTime("2025-01-01T00:00:00"), ZoneId.systemDefault()).days(10).build(); // P10D
        SequenceISO8601 s = new SequenceISO8601(rule);

        long after = UtilDate.getTime("2025-01-05T00:00:00");
        Assertions.assertEquals("2025-01-11T00:00:00.000", UtilDate.msFormat(s.next(after)));

        after = UtilDate.getTime("2025-01-11T00:00:00");
        Assertions.assertEquals("2025-01-21T00:00:00.000", UtilDate.msFormat(s.next(after)));
    }

    // ---------------- Mixed Period + Duration (накопительный) ----------------

    @Test
    public void mixedStep_monthPlus12Hours_isAccumulative() throws ParseException {

        TemplateISO8601 rule = TemplateISO8601.builder(UtilDate.getTime("2025-01-01T00:00:00"), ZoneId.systemDefault()).months(1).hours(12).build(); // P1MT12H
        SequenceISO8601 s = new SequenceISO8601(rule);

        // после 2025-02-01 12:00 следующий тик:
        // 2025-01-01 00:00 + 1M + 12H = 2025-02-01 12:00 (НЕ подходит, нужно строго AFTER)
        // + (1M + 12H) => 2025-03-02 00:00
        long after = UtilDate.getTime("2025-02-01T12:00:00");
        Assertions.assertEquals("2025-03-02T00:00:00.000", UtilDate.msFormat(s.next(after)));
    }

    @Test
    public void mixedStep_dayPlus90Minutes() throws ParseException {
        TemplateISO8601 rule = TemplateISO8601.builder(UtilDate.getTime("2025-01-01T00:00:00"), ZoneId.systemDefault()).days(1).minutes(90).build(); // P1DT90M
        SequenceISO8601 s = new SequenceISO8601(rule);

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
        // дата вокруг DST (2025-03-30)
        TemplateISO8601 rule = TemplateISO8601.builder(UtilDate.getTime("2025-03-30T00:00:00"), ZoneId.of("Europe/Amsterdam")).hours(1).build();     // PT1H
        SequenceISO8601 s = new SequenceISO8601(rule);

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
        // дата вокруг DST (2025-10-26)
        TemplateISO8601 rule = TemplateISO8601.builder(UtilDate.getTime("2025-10-26T00:00:00"), ZoneId.of("Europe/Amsterdam")).hours(1).build();     // PT1H
        SequenceISO8601 s = new SequenceISO8601(rule);

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

        Assertions.assertThrows(ForwardException.class,
                () -> new SequenceISO8601(
                        TemplateISO8601.builder(UtilDate.getTime("2025-01-01T00:00:00"), ZoneId.systemDefault())
                                .build()
                )
        );
    }

    @Test
    public void guardMaxIterationsTripsOnWhileLoop() throws ParseException {
        ZoneId zone = ZoneId.systemDefault();

        long start = UtilDate.getTime("2025-01-01T00:00:00");

        // after на 1000 дней вперед (чтобы нужно было много итераций)
        long after = start + 1000L * 24 * 60 * 60 * 1000;

        // period != 0 и duration != 0 -> fast-path и fastForwardPeriodOnly НЕ сработают,
        // значит попадем в while и начнем крутить итерации.
        TemplateISO8601 rule = TemplateISO8601.builder(start, zone)
                .days(1)
                .seconds(1)
                .build(); // "P1DT1S"

        SequenceISO8601 s = new SequenceISO8601(rule);
        s.setGuardMaxIterations(10);

        Assertions.assertThrows(ForwardException.class, () -> s.next(after));
    }

}