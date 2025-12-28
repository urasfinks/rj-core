package ru.jamsys.core.flat.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.date.UtilDate;
import ru.jamsys.core.flat.util.date.UtilDateBetween;

class UtilDateBetweenTest {
    @Test
    void ex() {
        String timeBetween = UtilDateBetween
                .betweenEpochMillis(1733916800, 1733916800)
                .getDescription(6, UtilDateBetween.StyleDescription.FORMAL);
        Assertions.assertNull(timeBetween);
    }

    @Test
    void timeBetween() {
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
                }""", UtilJson.toStringPretty(UtilDateBetween.betweenEpochMillis(1732906800, 1732916800), "{}"));

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
                }""", UtilJson.toStringPretty(UtilDateBetween.betweenEpochMillis(1732906800, 1733916800), "{}"));
        UtilDateBetween.TimeBetween timeBetween = UtilDateBetween.betweenEpochMillis(1732906800, 1733916800);
        Assertions.assertEquals("16 минут", timeBetween.getDescription(1, UtilDateBetween.StyleDescription.STANDARD));
        Assertions.assertEquals("16 минут", timeBetween.getDescription(1, UtilDateBetween.StyleDescription.FORMAL));
        Assertions.assertEquals("16 минут 50 секунд", timeBetween.getDescription(2, UtilDateBetween.StyleDescription.STANDARD));
        Assertions.assertEquals("16 минут и 50 секунд", timeBetween.getDescription(2, UtilDateBetween.StyleDescription.FORMAL));
        Assertions.assertEquals("16 минут 50 секунд", timeBetween.getDescription(3, UtilDateBetween.StyleDescription.STANDARD));

        Assertions.assertEquals("3 часа, 20 минут и 10 секунд", UtilDateBetween.betweenEpochMillis(1732906800, 1744916800).getDescription(6, UtilDateBetween.StyleDescription.FORMAL));
        Assertions.assertEquals("1 день, 7 часов, 6 минут и 50 секунд", UtilDateBetween.betweenEpochMillis(1732906800, 1844916800).getDescription(6, UtilDateBetween.StyleDescription.FORMAL));
        Assertions.assertEquals("3 года, 1 месяц, 8 дней, 13 часов, 20 минут и 10 секунд", UtilDateBetween.betweenEpochMillis(1732906800L, 99844916800L).getDescription(6, UtilDateBetween.StyleDescription.FORMAL));
        Assertions.assertEquals("3 года, 1 месяц, 8 дней, 13 часов и 20 минут", UtilDateBetween.betweenEpochMillis(1732906800L, 99844916800L).getDescription(5, UtilDateBetween.StyleDescription.FORMAL));
        Assertions.assertEquals("3 года, 1 месяц, 8 дней и 13 часов", UtilDateBetween.betweenEpochMillis(1732906800L, 99844916800L).getDescription(4, UtilDateBetween.StyleDescription.FORMAL));
        Assertions.assertEquals("3 года, 1 месяц и 8 дней", UtilDateBetween.betweenEpochMillis(1732906800L, 99844916800L).getDescription(3, UtilDateBetween.StyleDescription.FORMAL));
        Assertions.assertEquals("3 года и 1 месяц", UtilDateBetween.betweenEpochMillis(1732906800L, 99844916800L).getDescription(2, UtilDateBetween.StyleDescription.FORMAL));
        Assertions.assertEquals("3 года", UtilDateBetween.betweenEpochMillis(1732906800L, 99844916800L).getDescription(1, UtilDateBetween.StyleDescription.FORMAL));


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
                }""", UtilJson.toStringPretty(UtilDateBetween.betweenEpochMillis(173_773_097_324_7L, 173_786_040_000_0L), "{}"));

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
                }""", UtilJson.toStringPretty(UtilDateBetween.betweenEpochMillis(1737815629705L, 1737838800000L), "{}"));

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
                }""", UtilJson.toStringPretty(UtilDateBetween.betweenEpochMillis(
                UtilDate.date("2025-01-19").setPatternDate().toMillis().getMillis(),
                UtilDate.date("2025-01-20").setPatternDate().toMillis().getMillis()
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
                }""", UtilJson.toStringPretty(UtilDateBetween.betweenEpochMillis(
                UtilDate.date("2025-01-19").setPatternDate().toMillis().getMillis(),
                UtilDate.date("2025-01-21").setPatternDate().toMillis().getMillis()
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
                }""", UtilJson.toStringPretty(UtilDateBetween.betweenEpochMillis(
                UtilDate.date("2025-01-19 00:00:00").setPatternDateTime().compile().toMillis().getMillis(),
                UtilDate.date("2025-01-21 01:00:00").setPatternDateTime().compile().toMillis().getMillis()
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
                }""", UtilJson.toStringPretty(UtilDateBetween.betweenEpochMillis(
                UtilDate.date("2025-01-19 00:00:00").setPatternDateTime().compile().toMillis().getMillis(),
                UtilDate.date("2025-01-21 23:59:59").setPatternDateTime().compile().toMillis().getMillis()
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
                }""", UtilJson.toStringPretty(UtilDateBetween.betweenEpochMillis(
                UtilDate.date("2025-01-19 00:00:00").setPatternDateTime().compile().toMillis().getMillis(),
                UtilDate.date("2025-01-22 00:00:00").setPatternDateTime().compile().toMillis().getMillis()
        ), "{}"));

    }
}