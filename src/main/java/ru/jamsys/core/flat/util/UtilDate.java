package ru.jamsys.core.flat.util;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.builder.HashMapBuilder;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

public class UtilDate {

    public static String get(String format) {
        Date now = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(now);
    }

    public static long getTimestamp() {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        return timestamp.getTime() / 1000;
    }

    public static long getTimestamp(String date, String format) throws Exception {
        DateFormat dateFormat = new SimpleDateFormat(format);
        Date d1 = dateFormat.parse(date);
        return d1.getTime() / 1000;
    }

    public static long getTimestamp(String date, String format, long def) {
        try {
            DateFormat dateFormat = new SimpleDateFormat(format);
            Date d1 = dateFormat.parse(date);
            return d1.getTime() / 1000;
        } catch (Exception e) {
            App.error(e);
        }
        return def;
    }

    public static long getTimestampOffsetUTC() {
        ZoneId systemZone = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(systemZone);
        ZoneOffset offset = now.getOffset();
        return offset.getTotalSeconds();
    }

    public static long getMs(String date, String format) throws Exception {
        DateFormat dateFormat = new SimpleDateFormat(format);
        Date d1 = dateFormat.parse(date);
        return d1.getTime();
    }

    public static boolean validate(String date, String format) {
        DateFormat DATE_FORMAT = new SimpleDateFormat(format);
        DATE_FORMAT.setLenient(true);
        try {
            return DATE_FORMAT.format(DATE_FORMAT.parse(date)).equals(date);
        } catch (Exception e) {
            App.error(e);
        }
        return false;
    }

    public static String timestampFormat(long timestamp) {
        return timestampFormat(timestamp, "yyyy-MM-dd'T'HH:mm:ss");
    }

    public static long diffSecond(String date1, String date2, String format) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat(format);
        return (dateFormat.parse(date2).getTime() - dateFormat.parse(date1).getTime()) / 1000;
    }

    public static String timestampFormat(long timestamp, String format) {
        Timestamp stamp = new Timestamp(timestamp * 1000);
        return new SimpleDateFormat(format).format(new Date(stamp.getTime()));
    }

    public static String timestampFormatUTC(long timestamp, String format) {
        Timestamp ts = new Timestamp(timestamp * 1000);
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(ts);
    }

    public static String timestampFormatUTCOffset(long timestamp, String format, long offsetSec) {
        Timestamp ts = new Timestamp((timestamp + offsetSec) * 1000);
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(ts);
    }

    public static String msFormat(Long ms) {
        return msFormat(ms, "yyyy-MM-dd'T'HH:mm:ss.SSS");
    }

    public static String msFormat(Long ms, String format) {
        if (ms == null) {
            return null;
        }
        LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault());
        return date.format(DateTimeFormatter.ofPattern(format));
    }

    public static long getTimestampFromPostgreSql(String date) throws Exception {
        return getTimestamp(date, "yyyy-MM-dd HH:mm:ss.SSSX");
    }

    public static String format(String date, String format, String newFormat) throws ParseException {
        return new SimpleDateFormat(newFormat).format(new SimpleDateFormat(format).parse(date));
    }

    public static String format(String date, String format, String newFormat, String def) {
        try {
            return new SimpleDateFormat(newFormat).format(new SimpleDateFormat(format).parse(date));
        } catch (Exception e) {
            App.error(e);
        }
        return def;
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    @ToString
    @JsonPropertyOrder({"units", "description"})
    public static class TimeBetween {

        public enum Unit {
            years("год", "года", "лет"),
            months("месяц", "месяца", "месяцев"),
            days("день", "дня", "дней"),
            hours("час", "часа", "часов"),
            minutes("минута", "минуты", "минут"),
            seconds("секунда", "секунды", "секунд");
            final String one;
            final String two;
            final String five;

            Unit(String one, String two, String five) {
                this.one = one;
                this.two = two;
                this.five = five;
            }

            public String digitTranslate(long count) {
                return Util.digitTranslate(count, one, two, five);
            }

        }

        private Map<Unit, Long> units = new HashMapBuilder<Unit, Long>()
                .append(Unit.years, 0L)
                .append(Unit.months, 0L)
                .append(Unit.days, 0L)
                .append(Unit.hours, 0L)
                .append(Unit.minutes, 0L)
                .append(Unit.seconds, 0L);

        public TimeBetween set(Unit unit, int value) {
            return set(unit, (long) value);
        }

        public TimeBetween set(Unit unit, Long value) {
            units.put(unit, value);
            return this;
        }

        public String getDescription(int count) {
            StringBuilder sb = new StringBuilder();
            for (Unit unit : units.keySet()) {
                Long c = units.get(unit);
                if (c == 0) {
                    continue;
                }
                sb.append(c).append(" ").append(unit.digitTranslate(c)).append(" ");
                count--;
                if (count == 0) {
                    break;
                }
            }
            return sb.toString().trim();
        }

        public String getDescription() {
            return getDescription(6);
        }

    }

    public static TimeBetween getTimeBetween(long startTimestamp, long endTimestamp) {
        // Преобразуем Instant в LocalDateTime для удобной работы с датами
        LocalDateTime startDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTimestamp), ZoneId.systemDefault());
        LocalDateTime endDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(endTimestamp), ZoneId.systemDefault());

        // Определяем годы, месяцы и дни с использованием Period
        Period period = Period.between(startDateTime.toLocalDate(), endDateTime.toLocalDate());
        int years = period.getYears();
        int months = period.getMonths();
        int days = period.getDays();

        // Определяем часы, минуты и секунды с использованием Duration
        LocalDateTime adjustedStart = startDateTime.plusYears(years).plusMonths(months).plusDays(days);
        Duration duration = Duration.between(adjustedStart, endDateTime);

        return new TimeBetween()
                .set(TimeBetween.Unit.years, years)
                .set(TimeBetween.Unit.months, months)
                .set(TimeBetween.Unit.days, days)
                .set(TimeBetween.Unit.hours, duration.toHours())
                .set(TimeBetween.Unit.minutes, duration.toMinutes() % 60)
                .set(TimeBetween.Unit.minutes, duration.toMinutes() % 60)
                .set(TimeBetween.Unit.seconds, duration.getSeconds() % 60);

    }

}
