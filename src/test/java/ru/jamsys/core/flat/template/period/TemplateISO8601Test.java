package ru.jamsys.core.flat.template.period;

import org.junit.jupiter.api.Test;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.flat.template.scheduler.iso8601.TemplateISO8601;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Period;
import java.time.ZoneId;

class TemplateISO8601Test {

    @Test
    void builder_buildsDateOnly() {
        TemplateISO8601 iso = TemplateISO8601.builder(0, ZoneId.systemDefault())
                .years(1).months(2).days(3)
                .build();

        assertEquals("P1Y2M3D", iso.getIso());
        assertEquals(Period.of(1, 2, 3), iso.getPeriod());
        assertEquals(Duration.ZERO, iso.getDuration());
    }

    @Test
    void builder_buildsTimeOnlyWithoutSeconds() {
        TemplateISO8601 iso = TemplateISO8601.builder(0, ZoneId.systemDefault())
                .hours(12)
                .minutes(30)
                .build();

        assertEquals("PT12H30M", iso.getIso());
        assertEquals(Period.ZERO, iso.getPeriod());
        assertEquals(Duration.ofHours(12).plusMinutes(30), iso.getDuration());
    }

    @Test
    void builder_buildsMixedExample() {
        TemplateISO8601 iso = TemplateISO8601.builder(0, ZoneId.systemDefault())
                .months(1)
                .hours(12)
                .build();

        assertEquals("P1MT12H", iso.getIso());
        assertEquals(Period.ofMonths(1), iso.getPeriod());
        assertEquals(Duration.ofHours(12), iso.getDuration());
    }

    @Test
    void builder_buildsFractionalSecondsViaNanos() {
        TemplateISO8601 iso = TemplateISO8601.builder(0, ZoneId.systemDefault())
                .seconds(1)
                .nanos(500_000_000)
                .build();

        assertEquals("PT1.5S", iso.getIso());
        assertEquals(Period.ZERO, iso.getPeriod());
        assertEquals(Duration.ofSeconds(1).plusNanos(500_000_000), iso.getDuration());
    }

    @Test
    void parse_parsesDateOnly() {
        TemplateISO8601 iso = TemplateISO8601.parse("P1Y2M3D", 0, ZoneId.systemDefault());
        assertEquals(Period.of(1, 2, 3), iso.getPeriod());
        assertEquals(Duration.ZERO, iso.getDuration());
        assertEquals("P1Y2M3D", iso.getIso());
    }

    @Test
    void parse_parsesTimeOnly() {
        TemplateISO8601 iso = TemplateISO8601.parse("PT12H30M", 0, ZoneId.systemDefault());
        assertEquals(Period.ZERO, iso.getPeriod());
        assertEquals(Duration.ofHours(12).plusMinutes(30), iso.getDuration());
        assertEquals("PT12H30M", iso.getIso());
    }

    @Test
    void parse_parsesMixed() {
        TemplateISO8601 iso = TemplateISO8601.parse("P1MT12H", 0, ZoneId.systemDefault());
        assertEquals(Period.ofMonths(1), iso.getPeriod());
        assertEquals(Duration.ofHours(12), iso.getDuration());
        assertEquals("P1MT12H", iso.getIso());
    }

    @Test
    void parse_rejectsBlankAndNonIso() {
        assertThrows(ForwardException.class, () -> TemplateISO8601.parse(null, 0, ZoneId.systemDefault()));
        assertThrows(ForwardException.class, () -> TemplateISO8601.parse(" ", 0, ZoneId.systemDefault()));
        assertThrows(ForwardException.class, () -> TemplateISO8601.parse("1D", 0, ZoneId.systemDefault()));
        assertThrows(ForwardException.class, () -> TemplateISO8601.parse("R/2025-01-01/P1D", 0, ZoneId.systemDefault()));
    }

}