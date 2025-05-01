package ru.jamsys.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilLog;

import java.text.ParseException;

class UtilDateTest {

    @Test
    void t1() {
        long timestamp = UtilDate.getTimestamp();
        UtilLog.printInfo(timestamp);
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
    void timeBetween() throws Exception {
        Assertions.assertEquals("""
                {
                  "units" : {
                    "YEARS" : 0,
                    "MONTHS" : 0,
                    "DAYS" : 0,
                    "HOURS" : 0,
                    "MINUTES" : 0,
                    "SECONDS" : 10
                  },
                  "description" : "10 секунд"
                }""", UtilJson.toStringPretty(UtilDate.getTimeBetween(1732906800, 1732916800), "{}"));

        Assertions.assertEquals("""
                {
                  "units" : {
                    "YEARS" : 0,
                    "MONTHS" : 0,
                    "DAYS" : 0,
                    "HOURS" : 0,
                    "MINUTES" : 16,
                    "SECONDS" : 50
                  },
                  "description" : "16 минут 50 секунд"
                }""", UtilJson.toStringPretty(UtilDate.getTimeBetween(1732906800, 1733916800), "{}"));
        UtilDate.TimeBetween timeBetween = UtilDate.getTimeBetween(1732906800, 1733916800);
        Assertions.assertEquals("16 минут", timeBetween.getDescription(1, UtilDate.TimeBetween.StyleDescription.STANDARD));
        Assertions.assertEquals("16 минут", timeBetween.getDescription(1, UtilDate.TimeBetween.StyleDescription.FORMAL));
        Assertions.assertEquals("16 минут 50 секунд", timeBetween.getDescription(2, UtilDate.TimeBetween.StyleDescription.STANDARD));
        Assertions.assertEquals("16 минут и 50 секунд", timeBetween.getDescription(2, UtilDate.TimeBetween.StyleDescription.FORMAL));
        Assertions.assertEquals("16 минут 50 секунд", timeBetween.getDescription(3, UtilDate.TimeBetween.StyleDescription.STANDARD));

        Assertions.assertEquals("3 часа, 20 минут и 10 секунд", UtilDate.getTimeBetween(1732906800, 1744916800).getDescription(6, UtilDate.TimeBetween.StyleDescription.FORMAL));
        Assertions.assertEquals("1 день, 7 часов, 6 минут и 50 секунд", UtilDate.getTimeBetween(1732906800, 1844916800).getDescription(6, UtilDate.TimeBetween.StyleDescription.FORMAL));
        Assertions.assertEquals("3 года, 1 месяц, 8 дней, 13 часов, 20 минут и 10 секунд", UtilDate.getTimeBetween(1732906800L, 99844916800L).getDescription(6, UtilDate.TimeBetween.StyleDescription.FORMAL));
        Assertions.assertEquals("3 года, 1 месяц, 8 дней, 13 часов и 20 минут", UtilDate.getTimeBetween(1732906800L, 99844916800L).getDescription(5, UtilDate.TimeBetween.StyleDescription.FORMAL));
        Assertions.assertEquals("3 года, 1 месяц, 8 дней и 13 часов", UtilDate.getTimeBetween(1732906800L, 99844916800L).getDescription(4, UtilDate.TimeBetween.StyleDescription.FORMAL));
        Assertions.assertEquals("3 года, 1 месяц и 8 дней", UtilDate.getTimeBetween(1732906800L, 99844916800L).getDescription(3, UtilDate.TimeBetween.StyleDescription.FORMAL));
        Assertions.assertEquals("3 года и 1 месяц", UtilDate.getTimeBetween(1732906800L, 99844916800L).getDescription(2, UtilDate.TimeBetween.StyleDescription.FORMAL));
        Assertions.assertEquals("3 года", UtilDate.getTimeBetween(1732906800L, 99844916800L).getDescription(1, UtilDate.TimeBetween.StyleDescription.FORMAL));


        Assertions.assertEquals("""
                {
                  "units" : {
                    "YEARS" : 0,
                    "MONTHS" : 0,
                    "DAYS" : 1,
                    "HOURS" : 11,
                    "MINUTES" : 57,
                    "SECONDS" : 6
                  },
                  "description" : "1 день 11 часов 57 минут 6 секунд"
                }""", UtilJson.toStringPretty(UtilDate.getTimeBetween(173_773_097_324_7L, 173_786_040_000_0L), "{}"));

        Assertions.assertEquals("""
                {
                  "units" : {
                    "YEARS" : 0,
                    "MONTHS" : 0,
                    "DAYS" : 0,
                    "HOURS" : 6,
                    "MINUTES" : 26,
                    "SECONDS" : 10
                  },
                  "description" : "6 часов 26 минут 10 секунд"
                }""", UtilJson.toStringPretty(UtilDate.getTimeBetween(1737815629705L, 1737838800000L), "{}"));

        Assertions.assertEquals("""
                {
                  "units" : {
                    "YEARS" : 0,
                    "MONTHS" : 0,
                    "DAYS" : 1,
                    "HOURS" : 0,
                    "MINUTES" : 0,
                    "SECONDS" : 0
                  },
                  "description" : "1 день"
                }""", UtilJson.toStringPretty(UtilDate.getTimeBetween(
                UtilDate.getTimestamp("2025-01-19", "yyyy-mm-dd") * 1000,
                UtilDate.getTimestamp("2025-01-20", "yyyy-mm-dd") * 1000
        ), "{}"));

        Assertions.assertEquals("""
                {
                  "units" : {
                    "YEARS" : 0,
                    "MONTHS" : 0,
                    "DAYS" : 2,
                    "HOURS" : 0,
                    "MINUTES" : 0,
                    "SECONDS" : 0
                  },
                  "description" : "2 дня"
                }""", UtilJson.toStringPretty(UtilDate.getTimeBetween(
                UtilDate.getTimestamp("2025-01-19", "yyyy-mm-dd") * 1000,
                UtilDate.getTimestamp("2025-01-21", "yyyy-mm-dd") * 1000
        ), "{}"));

        Assertions.assertEquals("""
                {
                  "units" : {
                    "YEARS" : 0,
                    "MONTHS" : 0,
                    "DAYS" : 2,
                    "HOURS" : 1,
                    "MINUTES" : 0,
                    "SECONDS" : 0
                  },
                  "description" : "2 дня 1 час"
                }""", UtilJson.toStringPretty(UtilDate.getTimeBetween(
                UtilDate.getTimestamp("2025-01-19 00:00:00", "yyyy-MM-dd hh:mm:ss") * 1000,
                UtilDate.getTimestamp("2025-01-21 01:00:00", "yyyy-MM-dd hh:mm:ss") * 1000
        ), "{}"));

        Assertions.assertEquals("""
                {
                  "units" : {
                    "YEARS" : 0,
                    "MONTHS" : 0,
                    "DAYS" : 2,
                    "HOURS" : 23,
                    "MINUTES" : 59,
                    "SECONDS" : 59
                  },
                  "description" : "2 дня 23 часа 59 минут 59 секунд"
                }""", UtilJson.toStringPretty(UtilDate.getTimeBetween(
                UtilDate.getTimestamp("2025-01-19 00:00:00", "yyyy-MM-dd hh:mm:ss") * 1000,
                UtilDate.getTimestamp("2025-01-21 23:59:59", "yyyy-MM-dd hh:mm:ss") * 1000
        ), "{}"));

        Assertions.assertEquals("""
                {
                  "units" : {
                    "YEARS" : 0,
                    "MONTHS" : 0,
                    "DAYS" : 3,
                    "HOURS" : 0,
                    "MINUTES" : 0,
                    "SECONDS" : 0
                  },
                  "description" : "3 дня"
                }""", UtilJson.toStringPretty(UtilDate.getTimeBetween(
                UtilDate.getTimestamp("2025-01-19 00:00:00", "yyyy-MM-dd hh:mm:ss") * 1000,
                UtilDate.getTimestamp("2025-01-22 00:00:00", "yyyy-MM-dd hh:mm:ss") * 1000
        ), "{}"));

    }

}