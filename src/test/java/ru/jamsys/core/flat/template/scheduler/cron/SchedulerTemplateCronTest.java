package ru.jamsys.core.flat.template.scheduler.cron;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ru.jamsys.core.extension.exception.ForwardException;

import java.util.*;

class SchedulerTemplateCronTest {
// region title validation

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\n", "\t"})
    @DisplayName("Builder(String title): blank title -> IllegalArgumentException")
    void builderCtor_blankTitle_throws(String title) {
        assertThrows(IllegalArgumentException.class, () -> SchedulerTemplateCron.builder(title));
    }

    @Test
    @DisplayName("Builder(String title): null title -> IllegalArgumentException")
    void builderCtor_nullTitle_throws() {
        assertThrows(IllegalArgumentException.class, () -> SchedulerTemplateCron.builder(null));
    }

    @Test
    @DisplayName("@JsonCreator Builder(...): blank title -> IllegalArgumentException")
    void jsonCreatorCtor_blankTitle_throws() {
        assertThrows(IllegalArgumentException.class, () -> new SchedulerTemplateCron.Builder(
                "   ",
                null, null, null, null, null, null,
                null,
                null,
                null
        ));
    }

    // endregion

    // region range validation

    @Test
    @DisplayName("months: value < 1 -> ForwardException")
    void months_belowMin_throws() {
        SchedulerTemplateCron.Builder b = SchedulerTemplateCron.builder("t1")
                .setMonths(List.of(0));

        assertThrows(ForwardException.class, b::buildTemplate);
    }

    @Test
    @DisplayName("months: value > 12 -> ForwardException")
    void months_aboveMax_throws() {
        SchedulerTemplateCron.Builder b = SchedulerTemplateCron.builder("t1")
                .setMonths(List.of(13));

        assertThrows(ForwardException.class, b::buildTemplate);
    }

    @Test
    @DisplayName("days: value out of range -> ForwardException")
    void days_outOfRange_throws() {
        SchedulerTemplateCron.Builder b = SchedulerTemplateCron.builder("t1")
                .setDays(List.of(32));

        assertThrows(ForwardException.class, b::buildTemplate);
    }

    @Test
    @DisplayName("hours: value out of range -> ForwardException")
    void hours_outOfRange_throws() {
        SchedulerTemplateCron.Builder b = SchedulerTemplateCron.builder("t1")
                .setHours(List.of(24));

        assertThrows(ForwardException.class, b::buildTemplate);
    }

    @Test
    @DisplayName("minutes: value out of range -> ForwardException")
    void minutes_outOfRange_throws() {
        SchedulerTemplateCron.Builder b = SchedulerTemplateCron.builder("t1")
                .setMinutes(List.of(60));

        assertThrows(ForwardException.class, b::buildTemplate);
    }

    @Test
    @DisplayName("seconds: value out of range -> ForwardException")
    void seconds_outOfRange_throws() {
        SchedulerTemplateCron.Builder b = SchedulerTemplateCron.builder("t1")
                .setSeconds(List.of(60));

        assertThrows(ForwardException.class, b::buildTemplate);
    }

    @Test
    @DisplayName("quarters: value out of range -> ForwardException")
    void quarters_outOfRange_throws() {
        SchedulerTemplateCron.Builder b = SchedulerTemplateCron.builder("t1")
                .setQuarters(List.of(0));

        assertThrows(ForwardException.class, b::buildTemplate);
    }

    @Test
    @DisplayName("days_of_week: value out of range -> ForwardException")
    void daysOfWeek_outOfRange_throws() {
        SchedulerTemplateCron.Builder b = SchedulerTemplateCron.builder("t1")
                .setDaysOfWeek(List.of(8));

        assertThrows(ForwardException.class, b::buildTemplate);
    }

    @Test
    @DisplayName("range lists: contains null -> ForwardException")
    void rangeList_containsNull_throws() {
        SchedulerTemplateCron.Builder b = SchedulerTemplateCron.builder("t1")
                .setMonths(Arrays.asList(1, null, 12));

        assertThrows(ForwardException.class, b::buildTemplate);
    }

    // endregion

    // region exclude

    @Test
    @DisplayName("exclude size > 10_000 -> ForwardException")
    void exclude_tooLarge_throws() {
        List<Integer> exclude = new ArrayList<>(10_001);
        for (int i = 0; i < 10_001; i++) exclude.add(i);

        SchedulerTemplateCron.Builder b = SchedulerTemplateCron.builder("t1")
                .setExclude(exclude);

        assertThrows(ForwardException.class, b::buildTemplate);
    }

    @Test
    @DisplayName("exclude size == 10_000 -> OK")
    void exclude_atLimit_ok() {
        List<Integer> exclude = new ArrayList<>(10_000);
        for (int i = 0; i < 10_000; i++) exclude.add(i);

        SchedulerTemplateCron tpl = SchedulerTemplateCron.builder("t1")
                .setExclude(exclude)
                .buildTemplate();

        assertNotNull(tpl);
        assertEquals(10_000, tpl.getExclude().size());
    }

    // endregion

    // region build + json values

    @Test
    @DisplayName("buildTemplate(): sets fields and template JSON string is not blank")
    void buildTemplate_setsFields_andTemplateFilled() {
        SchedulerTemplateCron tpl = SchedulerTemplateCron.builder("myTitle")
                .setYears(List.of(2025))
                .setMonths(List.of(1, 12))
                .setDays(List.of(1, 15))
                .setHours(List.of(0, 23))
                .setMinutes(List.of(0, 30))
                .setSeconds(List.of(0, 59))
                .setQuarters(List.of(1, 4))
                .setDaysOfWeek(List.of(1, 7))
                .setExclude(List.of(100, 200))
                .buildTemplate();

        assertEquals("myTitle", tpl.getTitle());
        assertEquals(List.of(2025), tpl.getYears());
        assertEquals(List.of(1, 12), tpl.getMonths());
        assertEquals(List.of(1, 15), tpl.getDays());
        assertEquals(List.of(0, 23), tpl.getHours());
        assertEquals(List.of(0, 30), tpl.getMinutes());
        assertEquals(List.of(0, 59), tpl.getSeconds());
        assertEquals(List.of(1, 4), tpl.getQuarters());
        assertEquals(List.of(1, 7), tpl.getDaysOfWeek());
        assertEquals(List.of(100, 200), tpl.getExclude());

        assertNotNull(tpl.getTemplate());
        assertFalse(tpl.getTemplate().isBlank(), "template must be non-blank JSON string");
        assertTrue(tpl.getTemplate().contains("\"title\""), "template JSON should contain title field");
        assertTrue(tpl.getTemplate().contains("\"days_of_week\""), "template JSON should contain days_of_week field");
    }

    @Test
    @DisplayName("SchedulerTemplateCron#getJsonValue(): contains days_of_week and template/title keys")
    void template_getJsonValue_containsExpectedKeys() {
        SchedulerTemplateCron tpl = SchedulerTemplateCron.builder("myTitle")
                .setDaysOfWeek(List.of(1, 2))
                .buildTemplate();

        Object jsonVal = tpl.getJsonValue();
        assertInstanceOf(Map.class, jsonVal);

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) jsonVal;

        assertEquals("myTitle", map.get("title"));
        assertTrue(map.containsKey("template"));
        assertTrue(map.containsKey("days_of_week"));
        assertEquals(List.of(1, 2), map.get("days_of_week"));
    }

    @Test
    @DisplayName("Builder#getJsonValue(): contains days_of_week key and provided values")
    void builder_getJsonValue_containsDaysOfWeek() {
        SchedulerTemplateCron.Builder b = SchedulerTemplateCron.builder("t1")
                .setDaysOfWeek(List.of(3, 4));

        Object jsonVal = b.getJsonValue();
        assertInstanceOf(Map.class, jsonVal);

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) jsonVal;

        assertEquals("t1", map.get("title"));
        assertTrue(map.containsKey("days_of_week"));
        assertEquals(List.of(3, 4), map.get("days_of_week"));
    }

    // endregion

    // region fromMap

    @Test
    @DisplayName("Builder.fromMap(): converts map fields and passes validation")
    void fromMap_convertsAndBuilds() {
        Map<String, Object> input = new HashMap<>();
        input.put("title", "t-map");
        input.put("months", List.of(1, 2, 12));
        input.put("days_of_week", List.of(1, 7));
        input.put("hours", List.of(0, 23));

        SchedulerTemplateCron.Builder b = SchedulerTemplateCron.Builder.fromMap(input);
        SchedulerTemplateCron tpl = b.buildTemplate();

        assertEquals("t-map", tpl.getTitle());
        assertEquals(List.of(1, 2, 12), tpl.getMonths());
        assertEquals(List.of(1, 7), tpl.getDaysOfWeek());
        assertEquals(List.of(0, 23), tpl.getHours());
    }

    @Test
    @DisplayName("Builder.fromMap(): invalid days_of_week fails during conversion (IllegalArgumentException/ValueInstantiationException)")
    void fromMap_invalidDaysOfWeek_throwsDuringFromMap() {
        Map<String, Object> input = new HashMap<>();
        input.put("title", "t-map");
        input.put("days_of_week", List.of(0)); // invalid

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> SchedulerTemplateCron.Builder.fromMap(input)
        );

        // опционально: проверяем, что в цепочке причин есть ForwardException
        assertTrue(hasCause(ex));
    }

    private static boolean hasCause(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof ForwardException) return true;
            cur = cur.getCause();
        }
        return false;
    }
}