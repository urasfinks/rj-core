package ru.jamsys.core.flat.template.scheduler.interval;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.time.*;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.function.Executable;
import ru.jamsys.core.flat.util.UtilDate;

import static org.junit.jupiter.api.Assertions.*;

class SchedulerSequenceIntervalTest {

    private static SchedulerTemplateInterval tpl(long startEpochMillis, ZoneId zone, Period p, Duration d) {
        // В проде TemplateInterval создаётся через builder, но для тестов можно использовать builder,
        // чтобы сохранять контракт валидации.
        SchedulerTemplateInterval.Builder b = SchedulerTemplateInterval.builder(startEpochMillis, zone);
        // builder не принимает Period/Duration напрямую, зададим поля
        // (используем доступные сеттеры Lombok @Accessors(chain=true))
        b.setYears(p.getYears()).setMonths(p.getMonths()).setDays(p.getDays());
        // duration разложим на h/m/s/nanos
        long seconds = d.getSeconds();
        int nanos = d.getNano();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        // В Builder поля int, тесты держим в разумных пределах
        b.setHours((int) hours).setMinutes((int) minutes).setSeconds((int) secs).setNanos(nanos);
        return b.buildTemplate();
    }

    @SuppressWarnings("all")
    private static long epochMillis(ZoneId zone, int y, int mo, int d, int h, int mi, int s) {
        return ZonedDateTime.of(LocalDateTime.of(y, mo, d, h, mi, s), zone).toInstant().toEpochMilli();
    }

    @Test
    void next_whenAfterBeforeStart_returnsStart() {
        ZoneId zone = ZoneId.of("UTC");
        long start = epochMillis(zone, 2025, 1, 1, 0, 0, 0);

        SchedulerTemplateInterval t = tpl(start, zone, Period.ZERO, Duration.ofMinutes(5));
        SchedulerSequenceInterval seq = new SchedulerSequenceInterval(t);

        long after = start - 1;
        assertEquals(start, seq.next(after));
    }

    @Test
    void next_durationOnly_strictlyAfter_semantics() {
        ZoneId zone = ZoneId.of("UTC");
        long start = epochMillis(zone, 2025, 1, 1, 0, 0, 0);

        SchedulerTemplateInterval t = tpl(start, zone, Period.ZERO, Duration.ofMinutes(10));
        SchedulerSequenceInterval seq = new SchedulerSequenceInterval(t);

        // after == start -> next is start + 10m (strictly after)
        assertEquals(start + 10 * 60_000L, seq.next(start));

        // after чуть меньше первого шага -> всё равно первый шаг
        assertEquals(start + 10 * 60_000L, seq.next(start + 1));

        // after ровно на границе шага -> следующий шаг
        assertEquals(start + 20 * 60_000L, seq.next(start + 10 * 60_000L));
    }

    @Test
    void next_durationOnly_largeJump_isO1AndCorrect() {
        ZoneId zone = ZoneId.of("UTC");
        long start = epochMillis(zone, 2025, 1, 1, 0, 0, 0);

        Duration step = Duration.ofSeconds(2);
        SchedulerTemplateInterval t = tpl(start, zone, Period.ZERO, step);
        SchedulerSequenceInterval seq = new SchedulerSequenceInterval(t);

        long after = start + 123_456_789L;
        long res = seq.next(after);

        assertTrue(res > after);
        assertEquals(0, (res - start) % step.toMillis());
    }

    @Test
    void next_periodOnly_days_strictlyAfter() {
        ZoneId zone = ZoneId.of("UTC");
        long start = epochMillis(zone, 2025, 1, 1, 0, 0, 0);

        SchedulerTemplateInterval t = tpl(start, zone, Period.ofDays(2), Duration.ZERO);
        SchedulerSequenceInterval seq = new SchedulerSequenceInterval(t);

        // строго после -> +2 дня
        long expected = ZonedDateTime.ofInstant(Instant.ofEpochMilli(start), zone).plusDays(2).toInstant().toEpochMilli();
        assertEquals(expected, seq.next(start));
    }

    @Test
    void next_periodOnly_months_strictlyAfter_handlesMonthLength() {
        ZoneId zone = ZoneId.of("UTC");
        long start = epochMillis(zone, 2025, 1, 31, 0, 0, 0);

        // +1 month от 31 января = 28 февраля (или 29 в високосный)
        SchedulerTemplateInterval t = tpl(start, zone, Period.ofMonths(1), Duration.ZERO);
        SchedulerSequenceInterval seq = new SchedulerSequenceInterval(t);

        long res = seq.next(start);

        ZonedDateTime zStart = ZonedDateTime.ofInstant(Instant.ofEpochMilli(start), zone);
        long expected = zStart.plusMonths(1).toInstant().toEpochMilli();

        assertEquals(expected, res);
        assertTrue(res > start);
    }

    @Test
    void next_periodOnly_fastForward_months_bigJump() {
        ZoneId zone = ZoneId.of("UTC");
        long start = epochMillis(zone, 2000, 1, 1, 0, 0, 0);

        SchedulerTemplateInterval t = tpl(start, zone, Period.ofMonths(3), Duration.ZERO);
        SchedulerSequenceInterval seq = new SchedulerSequenceInterval(t);

        long after = epochMillis(zone, 2025, 12, 1, 0, 0, 0);
        long res = seq.next(after);

        assertTrue(res > after);

        // Проверим, что результат равен одному из кратных шагу от start (по календарным месяцам)
        ZonedDateTime z = ZonedDateTime.ofInstant(Instant.ofEpochMilli(start), zone);
        ZonedDateTime r = ZonedDateTime.ofInstant(Instant.ofEpochMilli(res), zone);
        long monthsBetween = ChronoUnit.MONTHS.between(z, r);
        assertEquals(0, monthsBetween % 3);
    }

    @Test
    void next_periodOnly_fastForward_days_bigJump() {
        ZoneId zone = ZoneId.of("UTC");
        long start = epochMillis(zone, 2000, 1, 1, 0, 0, 0);

        SchedulerTemplateInterval t = tpl(start, zone, Period.ofDays(10), Duration.ZERO);
        SchedulerSequenceInterval seq = new SchedulerSequenceInterval(t);

        long after = epochMillis(zone, 2025, 12, 1, 0, 0, 0);
        long res = seq.next(after);

        assertTrue(res > after);
        assertEquals(0, (res - start) % (10L * 24 * 3600_000L));
    }

    @Test
    void next_mixedPeriodAndDuration_advancesStrictlyAfter() {
        ZoneId zone = ZoneId.of("UTC");
        long start = epochMillis(zone, 2025, 1, 1, 0, 0, 0);

        // Шаг: +1 день + 2 часа
        SchedulerTemplateInterval t = tpl(start, zone, Period.ofDays(1), Duration.ofHours(2));
        SchedulerSequenceInterval seq = new SchedulerSequenceInterval(t);

        // строго после => start + 1d + 2h
        long expected = ZonedDateTime.ofInstant(Instant.ofEpochMilli(start), zone)
                .plusDays(1)
                .plusHours(2)
                .toInstant()
                .toEpochMilli();

        assertEquals(expected, seq.next(start));
    }

    @Test
    void next_forwardOnlyContract_throwsOnDecreasingAfter() {
        ZoneId zone = ZoneId.of("UTC");
        long start = epochMillis(zone, 2025, 1, 1, 0, 0, 0);

        SchedulerTemplateInterval t = tpl(start, zone, Period.ZERO, Duration.ofSeconds(1));
        SchedulerSequenceInterval seq = new SchedulerSequenceInterval(t);

        long a1 = start + 10_000;
        long a2 = start + 9_000;

        seq.next(a1);

        Executable call = () -> seq.next(a2);
        assertThrows(RuntimeException.class, call); // ForwardException extends RuntimeException (как правило)
    }

    @Test
    void next_monotonicResults_whenAfterNonDecreasing() {
        ZoneId zone = ZoneId.of("UTC");
        long start = epochMillis(zone, 2025, 1, 1, 0, 0, 0);

        SchedulerTemplateInterval t = tpl(start, zone, Period.ofDays(1), Duration.ZERO);
        SchedulerSequenceInterval seq = new SchedulerSequenceInterval(t);

        long r1 = seq.next(start);
        long r2 = seq.next(r1); // after == r1 -> строго после => следующий день
        long r3 = seq.next(r2);

        assertTrue(r1 < r2 && r2 < r3);
    }

    @Test
    void guardMaxIterations_exceeded_throws() {
        ZoneId zone = ZoneId.of("UTC");
        long start = epochMillis(zone, 2025, 1, 1, 0, 0, 0);

        // ВАЖНО: period != 0 и duration != 0 => next() всегда идёт в advanceUntilAfter (цикл с guard)
        SchedulerTemplateInterval t = tpl(start, zone, Period.ofDays(1), Duration.ofSeconds(1));
        SchedulerSequenceInterval seq = new SchedulerSequenceInterval(t);

        int guard = 5;
        seq.setGuardMaxIterations(guard);

        // Шаг цикла в UTC: +1 day + 1 second = 86401 seconds = 86_401_000 ms
        long stepMillis = 86_401_000L;

        // Сделаем так, чтобы потребовалось ровно (guard + 1) итераций:
        // after = start + (guard+1)*stepMillis - 1
        // Тогда currentOffset после guard итераций всё ещё <= after, и потребуется ещё одна итерация => exceed.
        long after = start + (long) (guard + 1) * stepMillis - 1;

        assertThrows(RuntimeException.class, () -> seq.next(after));
        // Если у вас доступен ForwardException:
        // assertThrows(ForwardException.class, () -> seq.next(after));
    }

    @Test
    void invalid_guardMaxIterations_throws() {
        ZoneId zone = ZoneId.of("UTC");
        long start = epochMillis(zone, 2025, 1, 1, 0, 0, 0);

        SchedulerTemplateInterval t = tpl(start, zone, Period.ZERO, Duration.ofSeconds(1));
        SchedulerSequenceInterval seq = new SchedulerSequenceInterval(t);

        assertThrows(RuntimeException.class, () -> seq.setGuardMaxIterations(0));
        assertThrows(RuntimeException.class, () -> seq.setGuardMaxIterations(-10));
    }

    @Test
    void durationBelow1ms_isRejected_inDurationOnlyBranch() {
        ZoneId zone = ZoneId.of("UTC");
        long start = epochMillis(zone, 2025, 1, 1, 0, 0, 0);

        // Duration.ofNanos(500_000) = 0.5ms -> toMillis() == 0 => должно упасть
        SchedulerTemplateInterval t = tpl(start, zone, Period.ZERO, Duration.ofNanos(500_000));
        SchedulerSequenceInterval seq = new SchedulerSequenceInterval(t);

        assertThrows(RuntimeException.class, () -> seq.next(start));
    }

    @Test
    void templateValidation_zeroStep_rejected() {
        ZoneId zone = ZoneId.of("UTC");
        long start = epochMillis(zone, 2025, 1, 1, 0, 0, 0);

        // Builder/build должен выбросить (period=0, duration=0)
        assertThrows(RuntimeException.class, () -> tpl(start, zone, Period.ZERO, Duration.ZERO));
    }

    @Test
    void periodOnly_mixedMonthsAndDays_noFastForwardButStillCorrect() {
        ZoneId zone = ZoneId.of("UTC");
        long start = epochMillis(zone, 2025, 1, 1, 0, 0, 0);

        // Mixed months+days: fastForwardPeriodOnly вернёт start без ускорения,
        // затем advanceUntilAfter всё равно дойдёт до корректного значения.
        SchedulerTemplateInterval t = tpl(start, zone, Period.of(0, 1, 1), Duration.ZERO); // +1 month +1 day
        SchedulerSequenceInterval seq = new SchedulerSequenceInterval(t);

        long after = epochMillis(zone, 2025, 6, 1, 0, 0, 0);
        long res = seq.next(after);

        assertTrue(res > after);

        // Проверим что res — это start + k*(P1M1D) в календарной арифметике (не по millis).
        // Сымитируем итеративно до <= небольшого количества шагов от after:
        OffsetDateTime cursor = OffsetDateTime.ofInstant(Instant.ofEpochMilli(start), zone);
        OffsetDateTime a = OffsetDateTime.ofInstant(Instant.ofEpochMilli(after), zone);
        int safety = 0;
        while (!cursor.isAfter(a) && safety++ < 10_000) {
            cursor = cursor.plusMonths(1).plusDays(1);
        }
        assertEquals(cursor.toInstant().toEpochMilli(), res);
    }

    @Test
    public void test() throws ParseException {
        SchedulerTemplateInterval build = new SchedulerTemplateInterval.Builder(
                UtilDate.date("2024-02-29T00:00:00").setPatternDateTimeT().toMillis().getMillis(),
                ZoneId.systemDefault()
        )
                .setYears(1)
                .buildTemplate();
        long next = new SchedulerSequenceInterval(build).next(UtilDate.date("2024-02-29T00:00:00").setPatternDateTimeT().toMillis().getMillis());
        Assertions.assertEquals("2025-02-28T00:00:00.000", UtilDate.millis(next).toDate().getDate());
    }
}