package ru.jamsys.core.flat.util.date;

import lombok.Getter;
import lombok.Setter;

import java.time.*;

@SuppressWarnings("unused")
public final class UtilDate {

    /**
     * Дефолтная зона проекта для интерпретации локальных дат/дат-времени без offset/zone в строке.
     */
    public static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();

    /**
     * Базовый паттерн проекта для локального date-time без зоны/offset.
     */
    public static final String DEFAULT_PATTERN = "uuuu-MM-dd'T'HH:mm:ss.SSS";

    private UtilDate() {
    }

    public static Date date(String date) {
        return new Date(date);
    }

    public static Millis millis(long epocheMillis) {
        return new Millis(epocheMillis);
    }

    public static Second second(long epocheMillis) {
        return new Second(epocheMillis);
    }

    public static class ExtendPattern<T extends ExtendPattern<T>> {

        protected String pattern = UtilDate.DEFAULT_PATTERN;

        @SuppressWarnings("unchecked")
        protected T self() {
            return (T) this;
        }

        public T setPattern(String pattern) {
            this.pattern = pattern;
            return self();
        }

        public T setPatternDate() {
            this.pattern = "uuuu-MM-dd";
            return self();
        }

        public T setPatternDateTimeT() {
            this.pattern = "uuuu-MM-dd'T'HH:mm:ss";
            return self();
        }

        public T setPatternDateTimeTMs() {
            this.pattern = "uuuu-MM-dd'T'HH:mm:ss.SSS";
            return self();
        }

        public T setPatternDateTime() {
            this.pattern = "uuuu-MM-dd HH:mm:ss";
            return self();
        }

        public T setPatternDateTimeMs() {
            this.pattern = "uuuu-MM-dd HH:mm:ss.SSS";
            return self();
        }

    }

    @Setter
    @Getter
    public static class ExtendZone<T extends ExtendZone<T>> extends ExtendPattern<T> {

        protected ZoneId zoneId = UtilDate.DEFAULT_ZONE;

        public T setZoneId(ZoneId zoneId) {
            this.zoneId = zoneId;
            return self();
        }

        public T setZoneId(String nameZone) {
            this.zoneId = ZoneId.of(nameZone);
            return self();
        }

        public T setZoneMoscow() {
            this.zoneId = ZoneId.of("Europe/Moscow");
            return self();
        }

        public T setZoneUTC() {
            this.zoneId = ZoneOffset.UTC;
            return self();
        }

    }

    @Setter
    @Getter
    public static class Millis extends ExtendZone<Millis> {

        private long millis;

        public Millis(long millis) {
            this.millis = millis;
        }

        public Date toDate() {
            return new Date(Instant.ofEpochMilli(millis))
                    .setPattern(pattern)
                    .setZoneId(zoneId);
        }

        public Millis offset(long millis) {
            this.millis += millis;
            return this;
        }

    }

    @Setter
    @Getter
    public static class Second extends ExtendZone<Second> {

        private long second;

        public Second(long second) {
            this.second = second;
        }

        public Date toDate() {
            return new Date(Instant.ofEpochSecond(second))
                    .setPattern(pattern)
                    .setZoneId(zoneId);
        }

        public Second offset(long second) {
            this.second += second;
            return this;
        }

    }

    public static class Date extends ExtendZone<Date> {

        private Instant instant;

        private String initDate;

        public String getDate() {
            if (instant == null && initDate != null) {
                instant = UtilDateInstant.parseToInstant(initDate, pattern, zoneId);
            }
            return UtilDateInstant.format(instant, pattern, zoneId);
        }

        public Date compile() {
            if (instant == null && initDate != null) {
                instant = UtilDateInstant.parseToInstant(initDate, pattern, zoneId);
            }
            return this;
        }

        public Date(Instant instant) {
            this.instant = instant;
        }

        public Date(String date) {
            this.initDate = date;
        }

        public Millis toMillis() {
            if (instant == null && initDate != null) {
                instant = UtilDateInstant.parseToInstant(initDate, pattern, zoneId);
            }
            assert instant != null;
            return new Millis(instant.toEpochMilli())
                    .setZoneId(zoneId)
                    .setPattern(pattern);
        }

        public Second toSecond() {
            if (instant == null && initDate != null) {
                instant = UtilDateInstant.parseToInstant(initDate, pattern, zoneId);
            }
            assert instant != null;
            return new Second(instant.getEpochSecond())
                    .setZoneId(zoneId)
                    .setPattern(pattern);
        }

    }

}
