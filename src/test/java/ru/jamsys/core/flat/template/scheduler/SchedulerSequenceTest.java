package ru.jamsys.core.flat.template.scheduler;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.template.scheduler.interval.SchedulerTemplateInterval;
import ru.jamsys.core.flat.util.UtilDateOld;

import java.text.ParseException;
import java.time.ZoneId;

class SchedulerSequenceTest {
    @Test
    public void testInterval() throws ParseException {
        Assertions.assertEquals("2025-12-27T13:32:45.000",
                UtilDateOld.msFormat(new SchedulerTemplateInterval.Builder(
                        UtilDateOld.getTime("2025-12-27T13:32:44.000"),
                        ZoneId.of("UTC")
                )
                        .setSeconds(1)
                        .buildSequence().next(UtilDateOld.getTime("2025-12-27T13:32:44.000")))
        );
        Assertions.assertEquals("2026-01-27T13:32:44.000",
                UtilDateOld.msFormat(new SchedulerTemplateInterval.Builder(
                        UtilDateOld.getTime("2025-12-27T13:32:44.000"),
                        ZoneId.of("UTC")
                )
                        .setMonths(1)
                        .buildSequence().next(UtilDateOld.getTime("2025-12-27T13:32:44.000")))
        );
        Assertions.assertEquals("2026-12-27T13:32:44.000",
                UtilDateOld.msFormat(new SchedulerTemplateInterval.Builder(
                        UtilDateOld.getTime("2025-12-27T13:32:44.000"),
                        ZoneId.of("UTC")
                )
                        .setYears(1)
                        .buildSequence().next(UtilDateOld.getTime("2025-12-27T13:32:44.000")))
        );
        Assertions.assertEquals("2025-02-28T13:32:44.000",
                UtilDateOld.msFormat(new SchedulerTemplateInterval.Builder(
                        UtilDateOld.getTime("2024-02-29T13:32:44.000"),
                        ZoneId.of("UTC")
                )
                        .setYears(1)
                        .buildSequence().next(UtilDateOld.getTime("2024-02-29T13:32:44.000")))
        );
    }
}