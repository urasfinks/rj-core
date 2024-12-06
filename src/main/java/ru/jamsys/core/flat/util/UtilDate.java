package ru.jamsys.core.flat.util;

import ru.jamsys.core.App;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
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

}
