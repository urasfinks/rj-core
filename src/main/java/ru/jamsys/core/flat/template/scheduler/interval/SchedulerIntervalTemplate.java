package ru.jamsys.core.flat.template.scheduler.interval;

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

import java.time.Duration;
import java.time.Period;
import java.time.ZoneId;
import java.util.Map;

@Getter
public class SchedulerIntervalTemplate {

    private final Period period;
    private final Duration duration;
    private final String template;
    private final long startEpochMillis;
    private final ZoneId zone;

    private Map<String, Object> getContext() {
        return new HashMapBuilder<String, Object>()
                .append("template", template)
                .append("period", Util.firstNonNull(period, "null").toString())
                .append("duration", Util.firstNonNull(duration, "null").toString())
                .append("zone", Util.firstNonNull(zone, "null").toString());
    }

    private SchedulerIntervalTemplate(long startEpochMillis, Period period, Duration duration, String template, ZoneId zone) {
        // Validate step
        if (period == null) {
            throw new ForwardException("period must not be null", getContext());
        }
        this.period = period;

        if (duration == null) {
            throw new ForwardException("duration must not be null", getContext());
        }
        this.duration = duration;

        if (zone == null) {
            throw new ForwardException("zone must not be null", getContext());
        }
        this.zone = zone;

        if (period.isZero() && duration.isZero()) {
            throw new ForwardException("Step must not be zero", getContext());
        }
        if (period.isNegative()) {
            throw new ForwardException("Period step must not be negative.", getContext());
        }
        if (duration.isNegative()) {
            throw new ForwardException("Duration step must not be negative.", getContext());
        }

        this.template = template;
        this.startEpochMillis = startEpochMillis;
    }

    @JsonValue
    public Object getJsonValue() {
        return getContext();
    }

    public static Builder builder(long startEpochMillis, ZoneId zone) {
        return new Builder(startEpochMillis, zone);
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    @JsonIgnoreProperties()
    public static class Builder {

        private final long startEpochMillis;

        private final ZoneId zone;

        private int years;
        private int months;
        private int days;

        private int hours;
        private int minutes;
        private int seconds;
        private int nanos;

        @JsonCreator
        public Builder(
                @JsonProperty(value = "startEpochMillis", required = true) long startEpochMillis,
                @JsonProperty(value = "zone", required = true) String zone,

                @JsonProperty("years") Integer years,
                @JsonProperty("months") Integer months,
                @JsonProperty("days") Integer days,

                @JsonProperty("hours") Integer hours,
                @JsonProperty("minutes") Integer minutes,
                @JsonProperty("seconds") Integer seconds,

                @JsonProperty("nanos") Integer nanos
        ) {
            if (zone == null || zone.isBlank()) {
                throw new IllegalArgumentException("zone is null/blank");
            }

            ZoneId parsedZone;
            try {
                parsedZone = ZoneId.of(zone);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid zone: " + zone, e);
            }

            if (startEpochMillis < 0) {
                throw new IllegalArgumentException("startEpochMillis must be >= 0");
            }

            checkNonNegative("years", years);
            checkNonNegative("months", months);
            checkNonNegative("days", days);
            checkNonNegative("hours", hours);
            checkNonNegative("minutes", minutes);
            checkNonNegative("seconds", seconds);

            int nnanos = nanos == null ? 0 : nanos;
            if (nnanos < 0 || nnanos > 999_999_999) {
                throw new IllegalArgumentException("nanos must be between 0 and 999,999,999");
            }

            this.startEpochMillis = startEpochMillis;
            this.zone = parsedZone;

            this.years = nz(years);
            this.months = nz(months);
            this.days = nz(days);

            this.hours = nz(hours);
            this.minutes = nz(minutes);
            this.seconds = nz(seconds);

            this.nanos = nnanos;
        }

        public Builder(long startEpochMillis, ZoneId zone) {
            this.startEpochMillis = startEpochMillis;
            this.zone = zone;
        }

        public Builder setNanos(int v) {
            if (v < 0 || v > 999_999_999) {
                throw new ForwardException("nanos must be between 0 and 999,999,999", this);
            }
            nanos = v;
            return this;
        }

        private static int nz(Integer v) {
            return v == null ? 0 : v;
        }

        private static void checkNonNegative(String name, Integer v) {
            if (v != null && v < 0) {
                throw new IllegalArgumentException(name + " must be >= 0");
            }
        }

        public static Builder fromJson(String json) throws Exception {
            ValidateType.JSON.validate(
                    json,
                    UtilFileResource.getAsString("schema/json/scheduler.template.iso-8601.json"),
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
                    .append("startEpochMillis", startEpochMillis)
                    .append("zone", Util.firstNonNull(zone, "null").toString())

                    .append("years", years)
                    .append("months", months)
                    .append("days", days)

                    .append("hours", hours)
                    .append("minutes", minutes)
                    .append("seconds", seconds)

                    .append("nanos", nanos)
                    ;
        }

        public SchedulerIntervalTemplate build() {
            Period p = Period.of(years, months, days);
            Duration d = Duration.ofHours(hours)
                    .plusMinutes(minutes)
                    .plusSeconds(seconds)
                    .plusNanos(nanos);

            return new SchedulerIntervalTemplate(
                    startEpochMillis,
                    p,
                    d,
                    UtilJson.toStringPretty(getJsonValue(), "{}"),
                    zone
            );
        }

    }


}
