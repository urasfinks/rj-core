package ru.jamsys.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.UtilDate;

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
    void diffSecond() {
        Assertions.assertEquals(60L, UtilDate.diffSecond("3:18", "03:19", "H:mm"));
    }

}