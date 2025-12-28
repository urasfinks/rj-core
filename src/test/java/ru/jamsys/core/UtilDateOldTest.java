package ru.jamsys.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.flat.util.UtilDateOld;

import java.text.ParseException;
import java.time.ZoneOffset;

class UtilDateOldTest {

    @Test
    void format() {
        Assertions.assertEquals("2011-01-18", UtilDate.convert("2011-01-18 00:00:00.0", "uuuu-MM-dd HH:mm:ss.S", "yyyy-MM-dd"));
        Assertions.assertEquals("03.03.2025", UtilDate.convert("20250303", "uuuuMMdd", "dd.MM.yyyy"));
    }

    @Test
    void formatUTC() {
        Assertions.assertEquals(
                "2024-11-29 19:00:00",
                UtilDate.formatEpochSecond(
                        1732906800L,
                        "uuuu-MM-dd HH:mm:ss",
                        ZoneOffset.UTC
                )
        );
    }

    @Test
    void formatUTCOffset() {
        Assertions.assertEquals(
                "2024-11-29 14:00:00",
                UtilDate.formatEpochSecond(
                        1732906800L,
                        "uuuu-MM-dd HH:mm:ss",
                        ZoneOffset.ofHours(-5)
                )
        );
    }

}