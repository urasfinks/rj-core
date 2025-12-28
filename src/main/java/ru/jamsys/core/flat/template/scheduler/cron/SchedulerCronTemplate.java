package ru.jamsys.core.flat.template.scheduler.cron;

import com.fasterxml.jackson.annotation.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.validate.ValidateType;

import java.util.List;
import java.util.Map;

@Getter
public class SchedulerCronTemplate {

    private final String title;

    private final List<Integer> years;
    private final List<Integer> months;
    private final List<Integer> days;
    private final List<Integer> hours;
    private final List<Integer> minutes;
    private final List<Integer> seconds;

    private final List<Integer> exclude;

    private final List<Integer> quarters;

    @JsonProperty("days_of_week")
    private final List<Integer> daysOfWeek;

    private final String template;

    private Map<String, Object> getContext() {
        return new HashMapBuilder<String, Object>()
                .append("template", template)
                .append("title", Util.firstNonNull(title, "null").toString())
                .append("years", years)
                .append("months", months)
                .append("days", days)
                .append("hours", hours)
                .append("minutes", minutes)
                .append("seconds", seconds)
                .append("exclude", exclude)
                .append("quarters", quarters)
                .append("days_of_week", daysOfWeek);
    }

    private SchedulerCronTemplate(
            String title,
            List<Integer> years,
            List<Integer> months,
            List<Integer> days,
            List<Integer> hours,
            List<Integer> minutes,
            List<Integer> seconds,
            List<Integer> exclude,
            List<Integer> quarters,
            List<Integer> daysOfWeek,
            String template
    ) {
        // Validate
        if (title == null || title.isBlank()) {
            throw new ForwardException("title is null/blank", getContext());
        }

        validateRangeList("months", months, 1, 12);
        validateRangeList("days", days, 1, 31);
        validateRangeList("hours", hours, 0, 23);
        validateRangeList("minutes", minutes, 0, 59);
        validateRangeList("seconds", seconds, 0, 59);
        validateRangeList("quarters", quarters, 1, 4);
        validateRangeList("days_of_week", daysOfWeek, 1, 7);

        if (exclude != null && exclude.size() > 10_000) {
            throw new ForwardException("exclude is too large", getContext());
        }

        this.title = title;

        this.years = years;
        this.months = months;
        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;

        this.exclude = exclude;

        this.quarters = quarters;

        this.daysOfWeek = daysOfWeek;

        this.template = template;
    }

    private static void validateRangeList(String field, List<Integer> values, int min, int max) {
        if (values == null) return;
        for (Integer v : values) {
            if (v == null) {
                throw new ForwardException(field + " contains null", Map.of("field", field));
            }
            if (v < min || v > max) {
                throw new ForwardException(
                        field + " value out of range [" + min + ".." + max + "]: " + v,
                        Map.of("field", field, "min", min, "max", max, "value", v)
                );
            }
        }
    }

    @JsonValue
    public Object getJsonValue() {
        return getContext();
    }

    public static Builder builder(String title) {
        return new Builder(title);
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    @JsonIgnoreProperties()
    public static class Builder {

        private final String title;

        private List<Integer> years;
        private List<Integer> months;
        private List<Integer> days;
        private List<Integer> hours;
        private List<Integer> minutes;
        private List<Integer> seconds;

        private List<Integer> exclude;

        private List<Integer> quarters;

        @JsonProperty("days_of_week")
        private List<Integer> daysOfWeek;

        public Builder(String title) {
            if (title == null || title.isBlank()) {
                throw new IllegalArgumentException("title is null/blank");
            }
            this.title = title;
        }

        @JsonCreator
        public Builder(
                @JsonProperty(value = "title", required = true) String title,

                @JsonProperty("years") List<Integer> years,
                @JsonProperty("months") List<Integer> months,
                @JsonProperty("days") List<Integer> days,
                @JsonProperty("hours") List<Integer> hours,
                @JsonProperty("minutes") List<Integer> minutes,
                @JsonProperty("seconds") List<Integer> seconds,

                @JsonProperty("exclude") List<Integer> exclude,

                @JsonProperty("quarters") List<Integer> quarters,

                @JsonProperty("days_of_week") List<Integer> daysOfWeek
        ) {
            if (title == null || title.isBlank()) {
                throw new IllegalArgumentException("title is null/blank");
            }

            // Валидация на этапе builder'а, чтобы ошибки ловились при парсинге/конвертации
            validateRangeList("months", months, 1, 12);
            validateRangeList("days", days, 1, 31);
            validateRangeList("hours", hours, 0, 23);
            validateRangeList("minutes", minutes, 0, 59);
            validateRangeList("seconds", seconds, 0, 59);
            validateRangeList("quarters", quarters, 1, 4);
            validateRangeList("days_of_week", daysOfWeek, 1, 7);

            if (exclude != null && exclude.size() > 10_000) {
                throw new IllegalArgumentException("exclude is too large");
            }

            this.title = title;

            this.years = years;
            this.months = months;
            this.days = days;
            this.hours = hours;
            this.minutes = minutes;
            this.seconds = seconds;

            this.exclude = exclude;

            this.quarters = quarters;

            this.daysOfWeek = daysOfWeek;
        }

        public static Builder fromJson(String json) throws Exception {
            ValidateType.JSON.validate(
                    json,
                    UtilFileResource.getAsString("schema/json/scheduler.template.json"),
                    null
            );
            return UtilJson.objectMapper.readValue(json, Builder.class);
        }

        public static Builder fromMap(Map<String, Object> map) {
            return UtilJson.objectMapper.convertValue(map, Builder.class);
        }

        @JsonValue
        public Object getJsonValue() {
            return new HashMapBuilder<String, Object>()
                    .append("title", title)

                    .append("years", years)
                    .append("months", months)
                    .append("days", days)
                    .append("hours", hours)
                    .append("minutes", minutes)
                    .append("seconds", seconds)

                    .append("exclude", exclude)

                    .append("quarters", quarters)

                    .append("days_of_week", daysOfWeek);
        }

        public SchedulerCronTemplate build() {
            return new SchedulerCronTemplate(
                    title,
                    years,
                    months,
                    days,
                    hours,
                    minutes,
                    seconds,
                    exclude,
                    quarters,
                    daysOfWeek,
                    UtilJson.toStringPretty(getJsonValue(), "{}")
            );
        }
    }
}
