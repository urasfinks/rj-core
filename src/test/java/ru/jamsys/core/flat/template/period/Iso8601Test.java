package ru.jamsys.core.flat.template.period;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Period;

class Iso8601Test {

    @Test
    void builder_buildsZeroWhenNothingSet() {
        Iso8601 iso = Iso8601.builder().build();
        assertEquals("PT0S", iso.getIso());
        assertEquals(Period.ZERO, iso.getPeriod());
        assertEquals(Duration.ZERO, iso.getDuration());
    }

    @Test
    void builder_buildsDateOnly() {
        Iso8601 iso = Iso8601.builder()
                .years(1).months(2).days(3)
                .build();

        assertEquals("P1Y2M3D", iso.getIso());
        assertEquals(Period.of(1, 2, 3), iso.getPeriod());
        assertEquals(Duration.ZERO, iso.getDuration());
    }

    @Test
    void builder_buildsTimeOnlyWithoutSeconds() {
        Iso8601 iso = Iso8601.builder()
                .hours(12)
                .minutes(30)
                .build();

        assertEquals("PT12H30M", iso.getIso());
        assertEquals(Period.ZERO, iso.getPeriod());
        assertEquals(Duration.ofHours(12).plusMinutes(30), iso.getDuration());
    }

    @Test
    void builder_buildsMixedExample() {
        Iso8601 iso = Iso8601.builder()
                .months(1)
                .hours(12)
                .build();

        assertEquals("P1MT12H", iso.getIso());
        assertEquals(Period.ofMonths(1), iso.getPeriod());
        assertEquals(Duration.ofHours(12), iso.getDuration());
    }

    @Test
    void builder_buildsFractionalSecondsViaNanos() {
        Iso8601 iso = Iso8601.builder()
                .seconds(1)
                .nanos(500_000_000)
                .build();

        assertEquals("PT1.5S", iso.getIso());
        assertEquals(Period.ZERO, iso.getPeriod());
        assertEquals(Duration.ofSeconds(1).plusNanos(500_000_000), iso.getDuration());
    }

    @Test
    void parse_parsesDateOnly() {
        Iso8601 iso = Iso8601.parse("P1Y2M3D");
        assertEquals(Period.of(1, 2, 3), iso.getPeriod());
        assertEquals(Duration.ZERO, iso.getDuration());
        assertEquals("P1Y2M3D", iso.getIso());
    }

    @Test
    void parse_parsesTimeOnly() {
        Iso8601 iso = Iso8601.parse("PT12H30M");
        assertEquals(Period.ZERO, iso.getPeriod());
        assertEquals(Duration.ofHours(12).plusMinutes(30), iso.getDuration());
        assertEquals("PT12H30M", iso.getIso());
    }

    @Test
    void parse_parsesMixed() {
        Iso8601 iso = Iso8601.parse("P1MT12H");
        assertEquals(Period.ofMonths(1), iso.getPeriod());
        assertEquals(Duration.ofHours(12), iso.getDuration());
        assertEquals("P1MT12H", iso.getIso());
    }

    @Test
    void parse_rejectsBlankAndNonIso() {
        assertThrows(IllegalArgumentException.class, () -> Iso8601.parse(null));
        assertThrows(IllegalArgumentException.class, () -> Iso8601.parse(" "));
        assertThrows(IllegalArgumentException.class, () -> Iso8601.parse("1D"));
        assertThrows(IllegalArgumentException.class, () -> Iso8601.parse("R/2025-01-01/P1D"));
    }
}