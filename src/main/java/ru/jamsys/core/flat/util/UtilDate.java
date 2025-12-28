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
import java.util.*;

public class UtilDate {

    public static String format = "yyyy-MM-dd'T'HH:mm:ss";

    public static String get(String format) {
        Date now = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(now);
    }

    // Если использовать int - конец наступит 19 января 2038 года, 03:14:07 UTC
    public static long getTimestamp() {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        return timestamp.getTime() / 1000;
    }

    public static long getTimestamp(String date, String format) throws Exception {
        DateFormat dateFormat = new SimpleDateFormat(format);
        Date d1 = dateFormat.parse(date);
        return d1.getTime() / 1000;
    }

    public static long getTime(String date) throws ParseException {
        DateFormat dateFormat = new SimpleDateFormat(format);
        Date d1 = dateFormat.parse(date);
        return d1.getTime();
    }

    public static long getTime() {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        return timestamp.getTime();
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

        public enum StyleDescription {
            STANDARD,
            FORMAL
        }

        public enum Unit {
            YEARS("год", "года", "лет"),
            MONTHS("месяц", "месяца", "месяцев"),
            DAYS("день", "дня", "дней"),
            HOURS("час", "часа", "часов"),
            MINUTES("минута", "минуты", "минут"),
            SECONDS("секунда", "секунды", "секунд");

            final String one;
            final String two;
            final String five;

            Unit(String one, String two, String five) {
                this.one = one;
                this.two = two;
                this.five = five;
            }

            public String digitTranslate(long count) {
                return UtilText.digitTranslate(count, one, two, five);
            }

        }

        private Map<Unit, Long> units = new HashMapBuilder<Unit, Long>()
                .append(Unit.YEARS, 0L)
                .append(Unit.MONTHS, 0L)
                .append(Unit.DAYS, 0L)
                .append(Unit.HOURS, 0L)
                .append(Unit.MINUTES, 0L)
                .append(Unit.SECONDS, 0L);

        public TimeBetween set(Unit unit, int value) {
            return set(unit, (long) value);
        }

        public TimeBetween set(Unit unit, Long value) {
            units.put(unit, value);
            return this;
        }

        public String getDescription(int count, StyleDescription styleDescription) {
            List<String> list = new ArrayList<>();

            for (Unit unit : units.keySet()) {
                Long c = units.get(unit);
                if (c == 0) {
                    continue;
                }
                list.add(c + " " + unit.digitTranslate(c));
                count--;
                if (count == 0) {
                    break;
                }
            }
            switch (styleDescription) {
                case STANDARD -> {
                    return String.join(" ", list);
                }
                case FORMAL -> {
                    if (!list.isEmpty()) {
                        String last = list.removeLast();
                        if (list.isEmpty()) {
                            return last;
                        } else {
                            return String.join(", ", list) + " и " + last;
                        }
                    }
                }
            }
            return null;
        }

        public String getDescription() {
            return getDescription(6, StyleDescription.STANDARD);
        }

    }

    public static TimeBetween getTimeBetween(long startTimestamp, long endTimestamp) {
        // Преобразуем в LocalDateTime
        LocalDateTime startDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTimestamp), ZoneId.systemDefault());
        LocalDateTime endDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(endTimestamp), ZoneId.systemDefault());

        // Убедимся, что startDateTime всегда раньше endDateTime
        boolean isReversed = startDateTime.isAfter(endDateTime);
        if (isReversed) {
            LocalDateTime temp = startDateTime;
            startDateTime = endDateTime;
            endDateTime = temp;
        }

        // Рассчитываем разницу в годах, месяцах и днях
        Period period = Period.between(startDateTime.toLocalDate(), endDateTime.toLocalDate());
        int years = period.getYears();
        int months = period.getMonths();
        int days = period.getDays();

        // Корректируем дату на рассчитанные годы, месяцы и дни
        LocalDateTime adjustedStart = startDateTime.plusYears(years).plusMonths(months).plusDays(days);

        // Рассчитываем разницу в часах, минутах и секундах
        Duration remainingDuration = Duration.between(adjustedStart, endDateTime);

        // Корректируем дни, если часы отрицательные
        if (remainingDuration.isNegative()) {
            adjustedStart = adjustedStart.minusDays(1);
            remainingDuration = Duration.between(adjustedStart, endDateTime);
            days -= 1; // Уменьшаем дни на 1
        }

        long hours = remainingDuration.toHours();
        long minutes = remainingDuration.toMinutes() % 60;
        long seconds = remainingDuration.getSeconds() % 60;

        // Если порядок был обратным, делаем результат отрицательным
        if (isReversed) {
            years = -years;
            months = -months;
            days = -days;
            hours = -hours;
            minutes = -minutes;
            seconds = -seconds;
        }

        // Возвращаем результат
        return new TimeBetween()
                .set(TimeBetween.Unit.YEARS, years)
                .set(TimeBetween.Unit.MONTHS, months)
                .set(TimeBetween.Unit.DAYS, days)
                .set(TimeBetween.Unit.HOURS, hours)
                .set(TimeBetween.Unit.MINUTES, minutes)
                .set(TimeBetween.Unit.SECONDS, seconds);
    }

}
