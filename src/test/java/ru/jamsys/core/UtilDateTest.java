package ru.jamsys.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.flat.util.UtilJson;

import java.text.ParseException;

class UtilDateTest {

    @Test
    void t1() {
        long timestamp = UtilDate.getTimestamp();
        System.out.println(timestamp);
    }

    @Test
    void format() throws ParseException {
        Assertions.assertEquals("2011-01-18", UtilDate.format("2011-01-18 00:00:00.0", "yyyy-MM-dd hh:mm:ss", "yyyy-MM-dd"));
        Assertions.assertEquals("03.03.2025", UtilDate.format("20250303", "yyyyMMdd", "dd.MM.yyyy"));
    }

    @Test
    void formatUTC() {
        Assertions.assertEquals("2024-11-29 19:00:00", UtilDate.timestampFormatUTC(1732906800, "yyyy-MM-dd HH:mm:ss"));
    }

    @Test
    void formatUTCOffset() {
        Assertions.assertEquals("2024-11-29 14:00:00", UtilDate.timestampFormatUTCOffset(
                1732906800,
                "yyyy-MM-dd HH:mm:ss",
                -5 * 60 * 60
        ));
    }

    @Test
    void diffSecond() throws ParseException {
        Assertions.assertEquals(60L, UtilDate.diffSecond("3:18", "03:19", "H:mm"));
    }

    @Test
    void timeBetween() {
        Assertions.assertEquals("""
                {
                  "units" : {
                    "years" : 0,
                    "months" : 0,
                    "days" : 0,
                    "hours" : 0,
                    "minutes" : 0,
                    "seconds" : 10
                  },
                  "description" : "10 секунд"
                }""", UtilJson.toStringPretty(UtilDate.getTimeBetween(1732906800, 1732916800), "{}"));

        Assertions.assertEquals("""
                {
                  "units" : {
                    "years" : 0,
                    "months" : 0,
                    "days" : 0,
                    "hours" : 0,
                    "minutes" : 16,
                    "seconds" : 50
                  },
                  "description" : "16 минут 50 секунд"
                }""", UtilJson.toStringPretty(UtilDate.getTimeBetween(1732906800, 1733916800), "{}"));
        UtilDate.TimeBetween timeBetween = UtilDate.getTimeBetween(1732906800, 1733916800);
        Assertions.assertEquals("16 минут", timeBetween.getDescription(1));
        Assertions.assertEquals("16 минут 50 секунд", timeBetween.getDescription(2));
        Assertions.assertEquals("16 минут 50 секунд", timeBetween.getDescription(3));


        Assertions.assertEquals("""
                {
                  "units" : {
                    "years" : 0,
                    "months" : 0,
                    "days" : 2,
                    "hours" : 12,
                    "minutes" : 2,
                    "seconds" : 54
                  },
                  "description" : "2 дня 12 часов 2 минуты 54 секунды"
                }""", UtilJson.toStringPretty(UtilDate.getTimeBetween(173_773_097_324_7L, 173_786_040_000_0L), "{}"));
    }

}