package ru.jamsys.core.flat.util;

import ru.jamsys.core.App;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@SuppressWarnings("unused")
public class UtilDateOld {

    public static String format = "yyyy-MM-dd'T'HH:mm:ss";

    public static ZoneId defaultZone = ZoneId.of("Europe/Moscow");

    public static String get(String format) {
        return get(format, defaultZone);
    }

    public static String get(String format, ZoneId zoneId) {
        Date now = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        formatter.setTimeZone(TimeZone.getTimeZone(zoneId));
        return formatter.format(now);
    }

    // Если использовать int - конец наступит 19 января 2038 года, 03:14:07 UTC
    public static long getTimestamp() {
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        return timestamp.getTime() / 1000;
    }

    public static long getTimestamp(String date, String format) throws Exception {
        return getTimestamp(date, format, defaultZone);
    }

    public static long getTimestamp(String date, String format, ZoneId zoneId) throws Exception {
        DateFormat dateFormat = new SimpleDateFormat(format);
        dateFormat.setTimeZone(TimeZone.getTimeZone(zoneId));
        Date d1 = dateFormat.parse(date);
        return d1.getTime() / 1000;
    }

    public static long getTime(String date) throws ParseException {
        return getTime(date, format, defaultZone);
    }

    public static long getTime(String date, String format, ZoneId zoneId) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setTimeZone(TimeZone.getTimeZone(zoneId));
        Date parsed = sdf.parse(date);
        return parsed.getTime();
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
        ZonedDateTime now = ZonedDateTime.now(defaultZone);
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
        return msFormat(ms, "yyyy-MM-dd'T'HH:mm:ss.SSS", defaultZone);
    }

    public static String msFormat(Long ms, String format, ZoneId zone) {
        if (ms == null) {
            return null;
        }
        LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), zone);
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

}
