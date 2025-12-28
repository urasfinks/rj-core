package ru.jamsys.core.flat.template.scheduler.cron;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.UtilDateOld;

import java.text.ParseException;
import java.time.ZoneId;
import java.util.List;

class SchedulerSequenceCronTest {

    private static final ZoneId ZONE = UtilDateOld.defaultZone;

    @Test
    public void test() throws ParseException {
        SchedulerTemplateCron schedulerTemplateCron = SchedulerTemplateCron.builder("description")
                .buildTemplate();

        SchedulerSequenceCron schedulerSequenceCron = new SchedulerSequenceCron(schedulerTemplateCron, ZONE);
        Assertions.assertEquals(
                "2025-12-27T13:32:45.000",
                UtilDateOld.msFormat(schedulerSequenceCron.next(UtilDateOld.getTime("2025-12-27T13:32:44.000")))
        );
    }

    @Test
    public void test2() throws ParseException {
        SchedulerTemplateCron schedulerTemplateCron = SchedulerTemplateCron.builder("description")
                .setSeconds(List.of(47))
                .buildTemplate();

        SchedulerSequenceCron schedulerSequenceCron = new SchedulerSequenceCron(schedulerTemplateCron, ZONE);
        Assertions.assertEquals(
                "2025-12-27T13:32:47.000",
                UtilDateOld.msFormat(schedulerSequenceCron.next(UtilDateOld.getTime("2025-12-27T13:32:44.000")))
        );
    }

    @Test
    public void test3() throws ParseException {
        SchedulerTemplateCron schedulerTemplateCron = SchedulerTemplateCron.builder("description")
                .setMinutes(List.of(33))
                .buildTemplate();

        SchedulerSequenceCron schedulerSequenceCron = new SchedulerSequenceCron(schedulerTemplateCron, ZONE);
        Assertions.assertEquals(
                "2025-12-27T13:33:00.000",
                UtilDateOld.msFormat(schedulerSequenceCron.next(UtilDateOld.getTime("2025-12-27T13:32:44.000")))
        );
    }

    @Test
    public void test4() throws ParseException {
        SchedulerTemplateCron schedulerTemplateCron = SchedulerTemplateCron.builder("description")
                .setMinutes(List.of(33))
                .setSeconds(List.of(47))
                .buildTemplate();

        SchedulerSequenceCron schedulerSequenceCron = new SchedulerSequenceCron(schedulerTemplateCron, ZONE);
        Assertions.assertEquals(
                "2025-12-27T13:33:47.000",
                UtilDateOld.msFormat(schedulerSequenceCron.next(UtilDateOld.getTime("2025-12-27T13:32:44.000")))
        );
    }

    @Test
    public void test5() throws ParseException {
        SchedulerTemplateCron schedulerTemplateCron = SchedulerTemplateCron.builder("description")
                .setHours(List.of(12))
                .setMinutes(List.of(33))
                .setSeconds(List.of(47))
                .buildTemplate();

        SchedulerSequenceCron schedulerSequenceCron = new SchedulerSequenceCron(schedulerTemplateCron, ZONE);
        Assertions.assertEquals(
                "2025-12-28T12:33:47.000",
                UtilDateOld.msFormat(schedulerSequenceCron.next(UtilDateOld.getTime("2025-12-27T13:32:44.000")))
        );
    }

    @Test
    public void test6() throws ParseException {
        SchedulerTemplateCron schedulerTemplateCron = SchedulerTemplateCron.builder("description")
                .setHours(List.of(12))
                .setMinutes(List.of(33))
                .setSeconds(List.of(47))
                .setDaysOfWeek(List.of(1))
                .buildTemplate();

        SchedulerSequenceCron schedulerSequenceCron = new SchedulerSequenceCron(schedulerTemplateCron, ZONE);
        Assertions.assertEquals(
                "2025-12-29T12:33:47.000",
                UtilDateOld.msFormat(schedulerSequenceCron.next(UtilDateOld.getTime("2025-12-27T13:32:44.000")))
        );
    }

    @Test
    public void test7() throws ParseException {
        SchedulerTemplateCron schedulerTemplateCron = SchedulerTemplateCron.builder("description")
                .setHours(List.of(12))
                .setMinutes(List.of(33))
                .setSeconds(List.of(47))
                .setQuarters(List.of(1))
                .setDaysOfWeek(List.of(1))
                .buildTemplate();

        SchedulerSequenceCron schedulerSequenceCron = new SchedulerSequenceCron(schedulerTemplateCron, ZONE);
        Assertions.assertEquals(
                "2026-01-05T12:33:47.000",
                UtilDateOld.msFormat(schedulerSequenceCron.next(UtilDateOld.getTime("2025-12-27T13:32:44.000")))
        );
    }

    @Test
    public void test8() throws ParseException {
        SchedulerTemplateCron schedulerTemplateCron = SchedulerTemplateCron.builder("description")
                .setDays(List.of(1))
                .setHours(List.of(12))
                .setMinutes(List.of(33))
                .setSeconds(List.of(47))
                .setQuarters(List.of(1))
                .setDaysOfWeek(List.of(1))
                .buildTemplate();

        SchedulerSequenceCron schedulerSequenceCron = new SchedulerSequenceCron(schedulerTemplateCron, ZONE);
        Assertions.assertEquals(
                "2027-02-01T12:33:47.000",
                UtilDateOld.msFormat(schedulerSequenceCron.next(UtilDateOld.getTime("2025-12-27T13:32:44.000")))
        );
    }

    @Test
    public void test9() throws ParseException {
        SchedulerTemplateCron schedulerTemplateCron = SchedulerTemplateCron.builder("description")
                .setDays(List.of(28))
                .setHours(List.of(12))
                .setMinutes(List.of(33))
                .setSeconds(List.of(47))
                .setDaysOfWeek(List.of(7))
                .buildTemplate();

        SchedulerSequenceCron schedulerSequenceCron = new SchedulerSequenceCron(schedulerTemplateCron, ZONE);
        Assertions.assertEquals(
                "2025-12-28T12:33:47.000",
                UtilDateOld.msFormat(schedulerSequenceCron.next(UtilDateOld.getTime("2025-12-27T13:32:44.000")))
        );
    }

    @Test
    public void test10() throws ParseException {
        SchedulerTemplateCron schedulerTemplateCron = SchedulerTemplateCron.builder("description")
                .setDaysOfWeek(List.of(1))
                .buildTemplate();

        SchedulerSequenceCron schedulerSequenceCron = new SchedulerSequenceCron(schedulerTemplateCron, ZONE);
        Assertions.assertEquals(
                "2025-12-29T00:00:00.000",
                UtilDateOld.msFormat(schedulerSequenceCron.next(UtilDateOld.getTime("2025-12-27T13:32:44.000")))
        );
    }

    @Test
    public void test11() throws ParseException {
        SchedulerTemplateCron schedulerTemplateCron = SchedulerTemplateCron.builder("description")
                .setYears(List.of(2026))
                .buildTemplate();

        SchedulerSequenceCron schedulerSequenceCron = new SchedulerSequenceCron(schedulerTemplateCron, ZONE);
        Assertions.assertEquals(
                "2026-01-01T00:00:00.000",
                UtilDateOld.msFormat(schedulerSequenceCron.next(UtilDateOld.getTime("2025-12-27T13:32:44.000")))
        );
    }

    @Test
    public void test12() throws ParseException {
        SchedulerTemplateCron schedulerTemplateCron = SchedulerTemplateCron.builder("description")
                .setYears(List.of(2026))
                .setMonths(List.of(2))
                .buildTemplate();

        SchedulerSequenceCron schedulerSequenceCron = new SchedulerSequenceCron(schedulerTemplateCron, ZONE);
        Assertions.assertEquals(
                "2026-02-01T00:00:00.000",
                UtilDateOld.msFormat(schedulerSequenceCron.next(UtilDateOld.getTime("2025-12-27T13:32:44.000")))
        );
    }


}