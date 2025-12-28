package ru.jamsys.core.flat.template.scheduler.interval;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.flat.util.UtilJson;

import java.time.Duration;
import java.time.Period;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SchedulerIntervalTemplateTest {
    @Test
    public void test() throws Exception {
        SchedulerIntervalTemplate.Builder builder = SchedulerIntervalTemplate.Builder.fromJson("""
                {
                  "startEpochMillis": 1766827964,
                  "zone": "UTC"
                }""");
        Assertions.assertEquals("""
                {
                  "startEpochMillis" : 1766827964,
                  "zone" : "UTC",
                  "years" : 0,
                  "months" : 0,
                  "days" : 0,
                  "hours" : 0,
                  "minutes" : 0,
                  "seconds" : 0,
                  "nanos" : 0
                }""", UtilJson.toStringPretty(builder.getJsonValue(), "{}"));

        builder.setDays(1);

        Assertions.assertEquals("""
                {
                  "startEpochMillis" : 1766827964,
                  "zone" : "UTC",
                  "years" : 0,
                  "months" : 0,
                  "days" : 1,
                  "hours" : 0,
                  "minutes" : 0,
                  "seconds" : 0,
                  "nanos" : 0
                }""", UtilJson.toStringPretty(builder.getJsonValue(), "{}"));

    }

    @Test
    void builder_build_success_periodAndDurationAndZone() {
        ZoneId zone = ZoneId.of("UTC");
        long start = 1_700_000_000_000L;

        SchedulerIntervalTemplate t = SchedulerIntervalTemplate.builder(start, zone)
                .setYears(1).setMonths(2).setDays(3)
                .setHours(4).setMinutes(5).setSeconds(6)
                .setNanos(7)
                .build();

        assertEquals(start, t.getStartEpochMillis());
        assertEquals(zone, t.getZone());
        assertEquals(Period.of(1, 2, 3), t.getPeriod());
        assertEquals(Duration.ofHours(4).plusMinutes(5).plusSeconds(6).plusNanos(7), t.getDuration());

        assertNotNull(t.getTemplate());
        assertFalse(t.getTemplate().isBlank());
    }

    @Test
    void builder_build_rejectsZeroStep() {
        ZoneId zone = ZoneId.of("UTC");
        long start = 1_700_000_000_000L;

        assertThrows(ForwardException.class, () ->
                SchedulerIntervalTemplate.builder(start, zone)
                        .build()
        );
    }

    @Test
    void builder_setNanos_rejectsNegative() {
        ZoneId zone = ZoneId.of("UTC");
        long start = 1_700_000_000_000L;

        SchedulerIntervalTemplate.Builder b = SchedulerIntervalTemplate.builder(start, zone);

        assertThrows(ForwardException.class, () -> b.setNanos(-1));
    }

    @Test
    void builder_setNanos_rejectsTooLarge() {
        ZoneId zone = ZoneId.of("UTC");
        long start = 1_700_000_000_000L;

        SchedulerIntervalTemplate.Builder b = SchedulerIntervalTemplate.builder(start, zone);

        assertThrows(ForwardException.class, () -> b.setNanos(1_000_000_000));
    }

    @Test
    void builder_fromMap_success() {
        Map<String, Object> map = new HashMap<>();
        map.put("startEpochMillis", 1_700_000_000_000L);
        map.put("zone", "UTC");
        map.put("years", 0);
        map.put("months", 1);
        map.put("days", 0);
        map.put("hours", 2);
        map.put("minutes", 3);
        map.put("seconds", 4);
        map.put("nanos", 5);

        SchedulerIntervalTemplate.Builder b = SchedulerIntervalTemplate.Builder.fromMap(map);
        SchedulerIntervalTemplate t = b.build();

        assertEquals(ZoneId.of("UTC"), t.getZone());
        assertEquals(Period.of(0, 1, 0), t.getPeriod());
        assertEquals(Duration.ofHours(2).plusMinutes(3).plusSeconds(4).plusNanos(5), t.getDuration());
    }

    @Test
    void builder_jsonCreator_rejectsBlankZone() {
        assertThrows(IllegalArgumentException.class, () ->
                new SchedulerIntervalTemplate.Builder(
                        1_700_000_000_000L,
                        "   ",
                        0, 0, 0,
                        0, 0, 1,
                        0
                )
        );
    }

    @Test
    void builder_jsonCreator_rejectsInvalidZone() {
        assertThrows(IllegalArgumentException.class, () ->
                new SchedulerIntervalTemplate.Builder(
                        1_700_000_000_000L,
                        "No/Such_Zone",
                        0, 0, 0,
                        0, 0, 1,
                        0
                )
        );
    }

    @Test
    void builder_jsonCreator_rejectsNegativeStartEpochMillis() {
        assertThrows(IllegalArgumentException.class, () ->
                new SchedulerIntervalTemplate.Builder(
                        -1,
                        "UTC",
                        0, 0, 0,
                        0, 0, 1,
                        0
                )
        );
    }

    @Test
    void builder_jsonCreator_rejectsNegativeParts() {
        // years < 0
        assertThrows(IllegalArgumentException.class, () ->
                new SchedulerIntervalTemplate.Builder(
                        1_700_000_000_000L,
                        "UTC",
                        -1, 0, 0,
                        0, 0, 1,
                        0
                )
        );

        // minutes < 0
        assertThrows(IllegalArgumentException.class, () ->
                new SchedulerIntervalTemplate.Builder(
                        1_700_000_000_000L,
                        "UTC",
                        0, 0, 0,
                        0, -1, 1,
                        0
                )
        );
    }

    @Test
    void builder_jsonCreator_acceptsNullFieldsAsZero() {
        SchedulerIntervalTemplate.Builder b = new SchedulerIntervalTemplate.Builder(
                1_700_000_000_000L,
                "UTC",
                null, null, null,
                null, null, 1,
                null
        );

        SchedulerIntervalTemplate t = b.build();
        assertEquals(Period.ZERO, t.getPeriod());
        assertEquals(Duration.ofSeconds(1), t.getDuration());
    }

    @Test
    void builder_fromJson_validatesSchema_and_parses() throws Exception {
        // Важно: json должен соответствовать вашему schema/json/scheduler.template.iso-8601.json
        // Подставьте поля согласно схеме (обычно такие же, как в Builder).
        String json = """
                {
                  "startEpochMillis": 1700000000000,
                  "zone": "UTC",
                  "years": 0,
                  "months": 0,
                  "days": 1,
                  "hours": 0,
                  "minutes": 0,
                  "seconds": 0,
                  "nanos": 0
                }
                """;

        SchedulerIntervalTemplate.Builder b = SchedulerIntervalTemplate.Builder.fromJson(json);
        SchedulerIntervalTemplate t = b.build();

        assertEquals(1_700_000_000_000L, t.getStartEpochMillis());
        assertEquals(ZoneId.of("UTC"), t.getZone());
        assertEquals(Period.ofDays(1), t.getPeriod());
        assertEquals(Duration.ZERO, t.getDuration()); // но шаг не ноль, т.к. period=1 day
    }

    @Test
    void templateInterval_getJsonValue_containsExpectedKeys() {
        ZoneId zone = ZoneId.of("UTC");
        long start = 1_700_000_000_000L;

        SchedulerIntervalTemplate t = SchedulerIntervalTemplate.builder(start, zone)
                .setDays(1)
                .build();

        Object jsonValue = t.getJsonValue();
        assertTrue(jsonValue instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) jsonValue;

        assertEquals(t.getTemplate(), m.get("template"));
        assertEquals(t.getPeriod().toString(), m.get("period"));
        assertEquals(t.getDuration().toString(), m.get("duration"));
        assertEquals(t.getZone().toString(), m.get("zone"));
    }

    @Test
    void builder_getJsonValue_containsAllFields() {
        ZoneId zone = ZoneId.of("UTC");
        long start = 1_700_000_000_000L;

        SchedulerIntervalTemplate.Builder b = SchedulerIntervalTemplate.builder(start, zone)
                .setYears(1).setMonths(2).setDays(3)
                .setHours(4).setMinutes(5).setSeconds(6)
                .setNanos(7);

        Object jsonValue = b.getJsonValue();
        assertTrue(jsonValue instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) jsonValue;

        assertEquals(start, ((Number) m.get("startEpochMillis")).longValue());
        assertEquals(zone.toString(), m.get("zone"));

        assertEquals(1, ((Number) m.get("years")).intValue());
        assertEquals(2, ((Number) m.get("months")).intValue());
        assertEquals(3, ((Number) m.get("days")).intValue());

        assertEquals(4, ((Number) m.get("hours")).intValue());
        assertEquals(5, ((Number) m.get("minutes")).intValue());
        assertEquals(6, ((Number) m.get("seconds")).intValue());

        assertEquals(7, ((Number) m.get("nanos")).intValue());
    }

    @Test
    void build_rejectsNegativePeriodOrDuration_viaJsonCreatorPath() {
        // В TemplateInterval.Builder JsonCreator сам запрещает отрицательные компоненты,
        // так что негатив туда не попадёт; но этот тест подтверждает поведение.
        assertThrows(IllegalArgumentException.class, () ->
                new SchedulerIntervalTemplate.Builder(
                        1_700_000_000_000L,
                        "UTC",
                        0, 0, -1,      // days negative
                        0, 0, 1,
                        0
                )
        );
    }

    @Test
    void build_rejectsStepWherePeriodAndDurationBothZero_viaJsonCreatorPath() {
        SchedulerIntervalTemplate.Builder b = new SchedulerIntervalTemplate.Builder(
                1_700_000_000_000L,
                "UTC",
                0, 0, 0,
                0, 0, 0,
                0
        );

        assertThrows(ForwardException.class, b::build);
    }

}