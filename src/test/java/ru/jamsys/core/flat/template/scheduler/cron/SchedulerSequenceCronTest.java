package ru.jamsys.core.flat.template.scheduler.cron;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.date.UtilDate;

import java.text.ParseException;
import java.util.List;

class SchedulerSequenceCronTest {

    @Test
    public void test() throws ParseException {
        SchedulerTemplateCron schedulerTemplateCron = SchedulerTemplateCron.builder("description")
                .buildTemplate();

        SchedulerSequenceCron schedulerSequenceCron = new SchedulerSequenceCron(schedulerTemplateCron, UtilDate.DEFAULT_ZONE);
        Assertions.assertEquals(
                "2025-12-27T13:32:45.000",
                UtilDate.millis(schedulerSequenceCron.next(UtilDate.date("2025-12-27T13:32:44.000").toMillis().getMillis())).toDate().getDate()
        );
    }

    @Test
    public void test2() throws ParseException {
        SchedulerTemplateCron schedulerTemplateCron = SchedulerTemplateCron.builder("description")
                .setSeconds(List.of(47))
                .buildTemplate();

        SchedulerSequenceCron schedulerSequenceCron = new SchedulerSequenceCron(schedulerTemplateCron, UtilDate.DEFAULT_ZONE);
        Assertions.assertEquals(
                "2025-12-27T13:32:47.000",
                UtilDate.millis(schedulerSequenceCron.next(UtilDate.date("2025-12-27T13:32:44.000").toMillis().getMillis())).toDate().getDate()
        );
    }

    @Test
    public void test3() {
        SchedulerTemplateCron schedulerTemplateCron = SchedulerTemplateCron.builder("description")
                .setMinutes(List.of(33))
                .buildTemplate();

        SchedulerSequenceCron schedulerSequenceCron = new SchedulerSequenceCron(schedulerTemplateCron, UtilDate.DEFAULT_ZONE);
        Assertions.assertEquals(
                "2025-12-27T13:33:00.000",
                UtilDate.millis(schedulerSequenceCron.next(UtilDate.date("2025-12-27T13:32:44.000").toMillis().getMillis())).toDate().getDate()
        );
    }

    @Test
    public void test4() {
        SchedulerTemplateCron schedulerTemplateCron = SchedulerTemplateCron.builder("description")
                .setMinutes(List.of(33))
                .setSeconds(List.of(47))
                .buildTemplate();

        SchedulerSequenceCron schedulerSequenceCron = new SchedulerSequenceCron(schedulerTemplateCron, UtilDate.DEFAULT_ZONE);
        Assertions.assertEquals(
                "2025-12-27T13:33:47.000",
                UtilDate.millis(schedulerSequenceCron.next(UtilDate.date("2025-12-27T13:32:44.000").toMillis().getMillis())).toDate().getDate()
        );
    }

    @Test
    public void test5() {
        SchedulerTemplateCron schedulerTemplateCron = SchedulerTemplateCron.builder("description")
                .setHours(List.of(12))
                .setMinutes(List.of(33))
                .setSeconds(List.of(47))
                .buildTemplate();

        SchedulerSequenceCron schedulerSequenceCron = new SchedulerSequenceCron(schedulerTemplateCron, UtilDate.DEFAULT_ZONE);
        Assertions.assertEquals(
                "2025-12-28T12:33:47.000",
                UtilDate.millis(
                                schedulerSequenceCron.next(
                                        UtilDate.date("2025-12-27T13:32:44.000")
                                                //.setZoneMoscow()
                                                .toMillis()
                                                .getMillis()
                                )
                        )
                        .toDate()
                        //.setZoneMoscow()
                        .getDate()

        );
    }

    @Test
    public void test6() {
        SchedulerTemplateCron schedulerTemplateCron = SchedulerTemplateCron.builder("description")
                .setHours(List.of(12))
                .setMinutes(List.of(33))
                .setSeconds(List.of(47))
                .setDaysOfWeek(List.of(1))
                .buildTemplate();

        SchedulerSequenceCron schedulerSequenceCron = new SchedulerSequenceCron(schedulerTemplateCron, UtilDate.DEFAULT_ZONE);
        Assertions.assertEquals(
                "2025-12-29T12:33:47.000",
                UtilDate.millis(schedulerSequenceCron.next(UtilDate.date("2025-12-27T13:32:44.000").toMillis().getMillis())).toDate().getDate()
        );
    }

    @Test
    public void test7() {
        SchedulerTemplateCron schedulerTemplateCron = SchedulerTemplateCron.builder("description")
                .setHours(List.of(12))
                .setMinutes(List.of(33))
                .setSeconds(List.of(47))
                .setQuarters(List.of(1))
                .setDaysOfWeek(List.of(1))
                .buildTemplate();

        SchedulerSequenceCron schedulerSequenceCron = new SchedulerSequenceCron(schedulerTemplateCron, UtilDate.DEFAULT_ZONE);
        Assertions.assertEquals(
                "2026-01-05T12:33:47.000",
                UtilDate.millis(schedulerSequenceCron.next(UtilDate.date("2025-12-27T13:32:44.000").toMillis().getMillis())).toDate().getDate()
        );
    }

    @Test
    public void test8() {
        SchedulerTemplateCron schedulerTemplateCron = SchedulerTemplateCron.builder("description")
                .setDays(List.of(1))
                .setHours(List.of(12))
                .setMinutes(List.of(33))
                .setSeconds(List.of(47))
                .setQuarters(List.of(1))
                .setDaysOfWeek(List.of(1))
                .buildTemplate();

        SchedulerSequenceCron schedulerSequenceCron = new SchedulerSequenceCron(schedulerTemplateCron, UtilDate.DEFAULT_ZONE);
        Assertions.assertEquals(
                "2027-02-01T12:33:47.000",
                UtilDate.millis(schedulerSequenceCron.next(UtilDate.date("2025-12-27T13:32:44.000").toMillis().getMillis())).toDate().getDate()
        );
    }

    @Test
    public void test9() {
        SchedulerTemplateCron schedulerTemplateCron = SchedulerTemplateCron.builder("description")
                .setDays(List.of(28))
                .setHours(List.of(12))
                .setMinutes(List.of(33))
                .setSeconds(List.of(47))
                .setDaysOfWeek(List.of(7))
                .buildTemplate();

        SchedulerSequenceCron schedulerSequenceCron = new SchedulerSequenceCron(schedulerTemplateCron, UtilDate.DEFAULT_ZONE);
        Assertions.assertEquals(
                "2025-12-28T12:33:47.000",
                UtilDate.millis(schedulerSequenceCron.next(UtilDate.date("2025-12-27T13:32:44.000").toMillis().getMillis())).toDate().getDate()
        );
    }

    @Test
    public void test10() {
        SchedulerTemplateCron schedulerTemplateCron = SchedulerTemplateCron.builder("description")
                .setDaysOfWeek(List.of(1))
                .buildTemplate();

        SchedulerSequenceCron schedulerSequenceCron = new SchedulerSequenceCron(schedulerTemplateCron, UtilDate.DEFAULT_ZONE);
        Assertions.assertEquals(
                "2025-12-29T00:00:00.000",
                UtilDate.millis(schedulerSequenceCron.next(UtilDate.date("2025-12-27T13:32:44.000").toMillis().getMillis())).toDate().getDate()
        );
    }

    @Test
    public void test11() {
        SchedulerTemplateCron schedulerTemplateCron = SchedulerTemplateCron.builder("description")
                .setYears(List.of(2026))
                .buildTemplate();

        SchedulerSequenceCron schedulerSequenceCron = new SchedulerSequenceCron(schedulerTemplateCron, UtilDate.DEFAULT_ZONE);
        Assertions.assertEquals(
                "2026-01-01T00:00:00.000",
                UtilDate.millis(schedulerSequenceCron.next(UtilDate.date("2025-12-27T13:32:44.000").toMillis().getMillis())).toDate().getDate()
        );
    }

    @Test
    public void test12() {
        SchedulerTemplateCron schedulerTemplateCron = SchedulerTemplateCron.builder("description")
                .setYears(List.of(2026))
                .setMonths(List.of(2))
                .buildTemplate();

        SchedulerSequenceCron schedulerSequenceCron = new SchedulerSequenceCron(schedulerTemplateCron, UtilDate.DEFAULT_ZONE);
        Assertions.assertEquals(
                "2026-02-01T00:00:00.000",
                UtilDate.millis(schedulerSequenceCron.next(UtilDate.date("2025-12-27T13:32:44.000").toMillis().getMillis())).toDate().getDate()
        );
    }


}