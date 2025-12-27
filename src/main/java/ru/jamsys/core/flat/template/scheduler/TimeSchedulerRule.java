package ru.jamsys.core.flat.template.scheduler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.validate.ValidateType;

import java.util.List;
import java.util.Map;

@Getter
@JsonIgnoreProperties(ignoreUnknown = false)
public class TimeSchedulerRule {

    private final String title;
    private final long start;

    private final List<Integer> years;
    private final List<Integer> months;
    private final List<Integer> days;
    private final List<Integer> hours;
    private final List<Integer> minutes;
    private final List<Integer> seconds;

    private final Boolean truncateTime;

    private final Integer offset;

    private final List<Integer> exclude;

    private final List<Integer> quarters;

    private final List<Integer> daysOfWeek;

    @JsonCreator
    public TimeSchedulerRule(
            @JsonProperty(value = "title", required = true) String title,
            @JsonProperty(value = "start", required = true) Long start,

            @JsonProperty("years") List<Integer> years,
            @JsonProperty("months") List<Integer> months,
            @JsonProperty("days") List<Integer> days,
            @JsonProperty("hours") List<Integer> hours,
            @JsonProperty("minutes") List<Integer> minutes,
            @JsonProperty("seconds") List<Integer> seconds,

            @JsonProperty("truncate_time") Boolean truncateTime,

            @JsonProperty("offset") Integer offset,

            @JsonProperty("exclude") List<Integer> exclude,

            @JsonProperty("quarters") List<Integer> quarters,

            @JsonProperty("days_of_week") List<Integer> daysOfWeek
    ) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title is null/blank");
        }
        if (start == null || start < 0) {
            throw new IllegalArgumentException("start is null or negative");
        }

        // validate ranges (optional fields)
        validateRangeList("months", months, 1, 12);
        validateRangeList("days", days, 1, 31);
        validateRangeList("hours", hours, 0, 23);
        validateRangeList("minutes", minutes, 0, 59);
        validateRangeList("seconds", seconds, 0, 59);
        validateRangeList("quarters", quarters, 1, 4);
        validateRangeList("days_of_week", daysOfWeek, 1, 7);

        // optional sanity checks
        if (exclude != null && exclude.size() > 10_000) {
            throw new IllegalArgumentException("exclude is too large");
        }

        this.title = title;
        this.start = start;

        this.years = years;
        this.months = months;
        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
        this.seconds = seconds;

        this.truncateTime = truncateTime;

        this.offset = offset;

        this.exclude = exclude;

        this.quarters = quarters;

        this.daysOfWeek = daysOfWeek;
    }

    private static void validateRangeList(String field, List<Integer> values, int min, int max) {
        if (values == null) return;
        for (Integer v : values) {
            if (v == null) {
                throw new IllegalArgumentException(field + " contains null");
            }
            if (v < min || v > max) {
                throw new IllegalArgumentException(
                        field + " value out of range [" + min + ".." + max + "]: " + v
                );
            }
        }
    }

    public static TimeSchedulerRule fromJson(String json) throws Exception {
        ValidateType.JSON.validate(
                json,
                UtilFileResource.getAsString("schema/json/scheduler.template.json"),
                null
        );
        return UtilJson.objectMapper.readValue(json, TimeSchedulerRule.class);
    }

    public static TimeSchedulerRule fromJson(String json, String rootField) throws JsonProcessingException {
        JsonNode root = UtilJson.objectMapper.readTree(json);
        JsonNode node = root.at(rootField);
        return UtilJson.objectMapper.treeToValue(node, TimeSchedulerRule.class);
    }

    public static TimeSchedulerRule fromMap(Map<String, Object> map) {
        return UtilJson.objectMapper.convertValue(map, TimeSchedulerRule.class);
    }

}
