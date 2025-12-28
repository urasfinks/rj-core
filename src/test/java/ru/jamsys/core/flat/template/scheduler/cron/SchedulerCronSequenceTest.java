package ru.jamsys.core.flat.template.scheduler.cron;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.UtilDate;

import java.text.ParseException;
import java.time.ZoneId;
import java.util.List;

class SchedulerCronSequenceTest {

    private static final ZoneId ZONE = ZoneId.systemDefault();

    @Test
    public void test() throws ParseException {
        SchedulerCronTemplate schedulerCronTemplate = SchedulerCronTemplate.builder("description")
                .build();

        SchedulerCronSequence schedulerCronSequence = new SchedulerCronSequence(schedulerCronTemplate, ZONE);
        Assertions.assertEquals(
                "2025-12-27T13:32:45.000",
                UtilDate.msFormat(schedulerCronSequence.next(UtilDate.getTime("2025-12-27T13:32:44.000")))
        );
    }

    @Test
    public void test2() throws ParseException {
        SchedulerCronTemplate schedulerCronTemplate = SchedulerCronTemplate.builder("description")
                .setSeconds(List.of(47))
                .build();

        SchedulerCronSequence schedulerCronSequence = new SchedulerCronSequence(schedulerCronTemplate, ZONE);
        Assertions.assertEquals(
                "2025-12-27T13:32:47.000",
                UtilDate.msFormat(schedulerCronSequence.next(UtilDate.getTime("2025-12-27T13:32:44.000")))
        );
    }

    @Test
    public void test3() throws ParseException {
        SchedulerCronTemplate schedulerCronTemplate = SchedulerCronTemplate.builder("description")
                .setMinutes(List.of(33))
                .build();

        SchedulerCronSequence schedulerCronSequence = new SchedulerCronSequence(schedulerCronTemplate, ZONE);
        Assertions.assertEquals(
                "2025-12-27T13:33:00.000",
                UtilDate.msFormat(schedulerCronSequence.next(UtilDate.getTime("2025-12-27T13:32:44.000")))
        );
    }

    @Test
    public void test4() throws ParseException {
        SchedulerCronTemplate schedulerCronTemplate = SchedulerCronTemplate.builder("description")
                .setMinutes(List.of(33))
                .setSeconds(List.of(47))
                .build();

        SchedulerCronSequence schedulerCronSequence = new SchedulerCronSequence(schedulerCronTemplate, ZONE);
        Assertions.assertEquals(
                "2025-12-27T13:33:47.000",
                UtilDate.msFormat(schedulerCronSequence.next(UtilDate.getTime("2025-12-27T13:32:44.000")))
        );
    }

    @Test
    public void test5() throws ParseException {
        SchedulerCronTemplate schedulerCronTemplate = SchedulerCronTemplate.builder("description")
                .setHours(List.of(12))
                .setMinutes(List.of(33))
                .setSeconds(List.of(47))
                .build();

        SchedulerCronSequence schedulerCronSequence = new SchedulerCronSequence(schedulerCronTemplate, ZONE);
        Assertions.assertEquals(
                "2025-12-28T12:33:47.000",
                UtilDate.msFormat(schedulerCronSequence.next(UtilDate.getTime("2025-12-27T13:32:44.000")))
        );
    }

    @Test
    public void test6() throws ParseException {
        SchedulerCronTemplate schedulerCronTemplate = SchedulerCronTemplate.builder("description")
                .setHours(List.of(12))
                .setMinutes(List.of(33))
                .setSeconds(List.of(47))
                .setDaysOfWeek(List.of(1))
                .build();

        SchedulerCronSequence schedulerCronSequence = new SchedulerCronSequence(schedulerCronTemplate, ZONE);
        Assertions.assertEquals(
                "2025-12-29T12:33:47.000",
                UtilDate.msFormat(schedulerCronSequence.next(UtilDate.getTime("2025-12-27T13:32:44.000")))
        );
    }

    @Test
    public void test7() throws ParseException {
        SchedulerCronTemplate schedulerCronTemplate = SchedulerCronTemplate.builder("description")
                .setHours(List.of(12))
                .setMinutes(List.of(33))
                .setSeconds(List.of(47))
                .setQuarters(List.of(1))
                .setDaysOfWeek(List.of(1))
                .build();

        SchedulerCronSequence schedulerCronSequence = new SchedulerCronSequence(schedulerCronTemplate, ZONE);
        Assertions.assertEquals(
                "2026-01-05T12:33:47.000",
                UtilDate.msFormat(schedulerCronSequence.next(UtilDate.getTime("2025-12-27T13:32:44.000")))
        );
    }

    @Test
    public void test8() throws ParseException {
        SchedulerCronTemplate schedulerCronTemplate = SchedulerCronTemplate.builder("description")
                .setDays(List.of(1))
                .setHours(List.of(12))
                .setMinutes(List.of(33))
                .setSeconds(List.of(47))
                .setQuarters(List.of(1))
                .setDaysOfWeek(List.of(1))
                .build();

        SchedulerCronSequence schedulerCronSequence = new SchedulerCronSequence(schedulerCronTemplate, ZONE);
        Assertions.assertEquals(
                "2027-02-01T12:33:47.000",
                UtilDate.msFormat(schedulerCronSequence.next(UtilDate.getTime("2025-12-27T13:32:44.000")))
        );
    }

    @Test
    public void test9() throws ParseException {
        SchedulerCronTemplate schedulerCronTemplate = SchedulerCronTemplate.builder("description")
                .setDays(List.of(28))
                .setHours(List.of(12))
                .setMinutes(List.of(33))
                .setSeconds(List.of(47))
                .setDaysOfWeek(List.of(7))
                .build();

        SchedulerCronSequence schedulerCronSequence = new SchedulerCronSequence(schedulerCronTemplate, ZONE);
        Assertions.assertEquals(
                "2025-12-28T12:33:47.000",
                UtilDate.msFormat(schedulerCronSequence.next(UtilDate.getTime("2025-12-27T13:32:44.000")))
        );
    }

    @Test
    public void test10() throws ParseException {
        SchedulerCronTemplate schedulerCronTemplate = SchedulerCronTemplate.builder("description")
                .setDaysOfWeek(List.of(1))
                .build();

        SchedulerCronSequence schedulerCronSequence = new SchedulerCronSequence(schedulerCronTemplate, ZONE);
        Assertions.assertEquals(
                "2025-12-29T00:00:00.000",
                UtilDate.msFormat(schedulerCronSequence.next(UtilDate.getTime("2025-12-27T13:32:44.000")))
        );
    }

    @Test
    public void test11() throws ParseException {
        SchedulerCronTemplate schedulerCronTemplate = SchedulerCronTemplate.builder("description")
                .setYears(List.of(2026))
                .build();

        SchedulerCronSequence schedulerCronSequence = new SchedulerCronSequence(schedulerCronTemplate, ZONE);
        Assertions.assertEquals(
                "2026-01-01T00:00:00.000",
                UtilDate.msFormat(schedulerCronSequence.next(UtilDate.getTime("2025-12-27T13:32:44.000")))
        );
    }

    @Test
    public void test12() throws ParseException {
        SchedulerCronTemplate schedulerCronTemplate = SchedulerCronTemplate.builder("description")
                .setYears(List.of(2026))
                .setMonths(List.of(2))
                .build();

        SchedulerCronSequence schedulerCronSequence = new SchedulerCronSequence(schedulerCronTemplate, ZONE);
        Assertions.assertEquals(
                "2026-02-01T00:00:00.000",
                UtilDate.msFormat(schedulerCronSequence.next(UtilDate.getTime("2025-12-27T13:32:44.000")))
        );
    }


}