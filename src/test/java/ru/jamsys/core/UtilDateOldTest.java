package ru.jamsys.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.UtilDateOld;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilLog;

import java.text.ParseException;

class UtilDateOldTest {

    @Test
    void format() throws ParseException {
        Assertions.assertEquals("2011-01-18", UtilDateOld.format("2011-01-18 00:00:00.0", "yyyy-MM-dd hh:mm:ss", "yyyy-MM-dd"));
        Assertions.assertEquals("03.03.2025", UtilDateOld.format("20250303", "yyyyMMdd", "dd.MM.yyyy"));
    }

    @Test
    void formatUTC() {
        Assertions.assertEquals("2024-11-29 19:00:00", UtilDateOld.timestampFormatUTC(1732906800, "yyyy-MM-dd HH:mm:ss"));
    }

    @Test
    void formatUTCOffset() {
        Assertions.assertEquals("2024-11-29 14:00:00", UtilDateOld.timestampFormatUTCOffset(
                1732906800,
                "yyyy-MM-dd HH:mm:ss",
                -5 * 60 * 60
        ));
    }

    @Test
    void diffSecond() throws ParseException {
        Assertions.assertEquals(60L, UtilDateOld.diffSecond("3:18", "03:19", "H:mm"));
    }



}