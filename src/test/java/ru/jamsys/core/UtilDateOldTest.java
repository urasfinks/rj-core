package ru.jamsys.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.UtilDate;

class UtilDateOldTest {

    @Test
    void format() {
        Assertions.assertEquals("2011-01-18", UtilDate.date("2011-01-18 00:00:00.0")
                .setPattern("uuuu-MM-dd HH:mm:ss.S")
                .compile()
                .setPatternDate()
                .getDate()
        );
        Assertions.assertEquals("03.03.2025", UtilDate.date("20250303")
                .setPattern("uuuuMMdd")
                .compile()
                .setPattern("dd.MM.yyyy")
                .getDate()
        );
    }

    @Test
    void formatUTC() {
        Assertions.assertEquals(
                "2024-11-29 19:00:00",
                UtilDate.second(1732906800L)
                        .setZoneUTC()
                        .toDate()
                        .setPatternDateTime()
                        .getDate()

        );
    }

    @Test
    void formatUTCOffset() {
        Assertions.assertEquals(
                "2024-11-29 14:00:00",
                UtilDate.second(1732906800L)
                        .setZoneUTC()
                        .offset(-1 * 5 * 60 * 60)
                        .toDate()
                        .setPatternDateTime()
                        .getDate()
        );
    }

    @Test
    void test() {
        Assertions.assertEquals("2024-11-29T00:00:00.000", UtilDate.date("2024-11-29")
                .setZoneMoscow()
                .setPatternDate()
                .toMillis()
                .setZoneUTC()
                .offset(3 * 60 * 60 * 1000)
                .toDate()
                .setPatternDateTimeTMs()
                .getDate()
        );

        Assertions.assertEquals("2024-11-29T00:00:00.000", UtilDate.date("2024-11-29")
                .setPatternDate()
                .compile()
                .setPatternDateTimeTMs()
                .getDate()
        );
    }

}