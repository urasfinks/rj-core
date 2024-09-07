package ru.jamsys.core.flat.util;

import ru.jamsys.core.App;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class UtilDate {

    public static String getDate(String format) {
        Date now = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(now);
    }

    public static long getTimestamp(String date, String format) throws Exception {
        DateFormat dateFormat = new SimpleDateFormat(format);
        Date d1 = dateFormat.parse(date);
        return d1.getTime() / 1000;
    }

    public static long getTimestampFromPostgreSql(String date) throws Exception {
        return getTimestamp(date, "yyyy-MM-dd HH:mm:ss.SSSX");
    }

    public static boolean dateValidate(String date, String format) {
        DateFormat DATE_FORMAT = new SimpleDateFormat(format);
        DATE_FORMAT.setLenient(true);
        try {
            return DATE_FORMAT.format(DATE_FORMAT.parse(date)).equals(date);
        } catch (Exception e) {
            App.error(e);
        }
        return false;
    }

    public static String timestampToDateFormat(long timestamp, String format) {
        Timestamp stamp = new Timestamp(timestamp * 1000);
        return new SimpleDateFormat(format).format(new Date(stamp.getTime()));
    }

    public static String msToDataFormat(Long ms) {
        if (ms == null) {
            return "null";
        }
        LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault());
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
    }

}
