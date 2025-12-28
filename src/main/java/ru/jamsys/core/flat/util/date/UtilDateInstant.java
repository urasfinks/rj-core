package ru.jamsys.core.flat.util.date;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.Objects;

public class UtilDateInstant {

    /**
     * Создаёт строгий {@link DateTimeFormatter} по заданному pattern.
     * <p>
     * Для чего:
     * - Гарантирует строгий парсинг дат (без "подгонки" типа 2025-02-31 -> 2025-03-03).
     * - Унифицирует поведение парсинга и форматирования в проекте.
     * <p>
     * Важно:
     * - В паттернах month = {@code MM}, minutes = {@code mm}.
     * <p>
     * Пример:
     * <pre>{@code
     * DateTimeFormatter fmt = UtilDate.formatter("uuuu-MM-dd");
     * LocalDate d = LocalDate.parse("2025-01-19", fmt);
     * }</pre>
     */
    public static DateTimeFormatter formatter(String pattern) {
        Objects.requireNonNull(pattern, "pattern");
        return DateTimeFormatter.ofPattern(pattern).withResolverStyle(ResolverStyle.STRICT);
    }

    /* ----------------------------- NOW ----------------------------- */

    /**
     * Возвращает текущий момент времени как {@link Instant} (UTC-линия времени).
     * <p>
     * Для чего:
     * - Хранение/передача "абсолютного" времени без зон.
     * - Основа для epochSecond/epochMilli.
     * <p>
     * Пример:
     * <pre>{@code
     * Instant now = UtilDate.nowInstant();
     * }</pre>
     */
    public static Instant nowInstant() {
        return Instant.now();
    }

    /**
     * Возвращает текущее время в epoch seconds (секунды с 1970-01-01T00:00:00Z).
     * <p>
     * Для чего:
     * - Компактное хранение (например, в интеграциях/логах, где принято seconds).
     * <p>
     * Пример:
     * <pre>{@code
     * long sec = UtilDate.nowEpochSecond();
     * }</pre>
     */
    public static long nowEpochSecond() {
        return Instant.now().getEpochSecond();
    }

    /**
     * Возвращает текущее время в epoch millis (миллисекунды с 1970-01-01T00:00:00Z).
     * <p>
     * Для чего:
     * - Наиболее распространённый формат времени в Java/JS и в БД (BIGINT).
     * <p>
     * Пример:
     * <pre>{@code
     * long ms = UtilDate.nowEpochMilli();
     * }</pre>
     */
    public static long nowEpochMilli() {
        return Instant.now().toEpochMilli();
    }

    /* ---------------------------- FORMAT --------------------------- */

    /**
     * Форматирует {@link Instant} в строку в заданной зоне {@code zoneId} по {@code pattern}.
     * <p>
     * Для чего:
     * - Представление момента времени пользователю в нужной временной зоне.
     * <p>
     * Пример (Москва):
     * <pre>{@code
     * Instant t = Instant.ofEpochSecond(1737268800L);
     * String s = UtilDate.format(t, "uuuu-MM-dd'T'HH:mm:ss", UtilDate.DEFAULT_ZONE);
     * }</pre>
     */
    public static String format(Instant instant, String pattern, ZoneId zoneId) {
        Objects.requireNonNull(instant, "instant");
        Objects.requireNonNull(zoneId, "zoneId");
        return instant.atZone(zoneId).format(formatter(pattern));
    }

    public static String format(Instant instant, String patternd) {
        return format(instant, patternd, UtilDate.DEFAULT_ZONE);
    }

    public static String convert(String data, String oldPatternd, String newPatternd, ZoneId zoneId) {
        Instant instant = UtilDateInstant.parseToInstant(data, oldPatternd, zoneId);
        return UtilDateInstant.format(instant, newPatternd, zoneId);
    }

    public static String convert(String data, String oldPatternd, String newPatternd) {
        return convert(data, oldPatternd, newPatternd, UtilDate.DEFAULT_ZONE);
    }

    /**
     * Форматирует epochMillis в строку в заданной зоне по {@code pattern}.
     * <p>
     * Для чего:
     * - Когда данные хранятся/передаются как миллисекунды, но нужно вывести строкой.
     * <p>
     * Пример:
     * <pre>{@code
     * long ms = 1737268800000L;
     * String s = UtilDate.formatEpochMilli(ms, "uuuu-MM-dd HH:mm:ss", ZoneId.of("UTC"));
     * }</pre>
     */
    public static String formatEpochMilli(long epochMilli, String pattern, ZoneId zoneId) {
        return format(Instant.ofEpochMilli(epochMilli), pattern, zoneId);
    }

    public static String formatEpochMilli(long epochMilli) {
        return format(Instant.ofEpochMilli(epochMilli), UtilDate.DEFAULT_PATTERN, UtilDate.DEFAULT_ZONE);
    }

    /**
     * Форматирует epochSeconds в строку в заданной зоне по {@code pattern}.
     * <p>
     * Для чего:
     * - Когда данные хранятся/передаются как секунды, но нужно вывести строкой.
     * <p>
     * Пример:
     * <pre>{@code
     * long sec = 1737268800L;
     * String s = UtilDate.formatEpochSecond(sec, "uuuu-MM-dd HH:mm:ss", UtilDate.DEFAULT_ZONE);
     * }</pre>
     */
    public static String formatEpochSecond(long epochSecond, String pattern, ZoneId zoneId) {
        return format(Instant.ofEpochSecond(epochSecond), pattern, zoneId);
    }

    /**
     * Возвращает текущий момент, отформатированный в строку в заданной зоне.
     * <p>
     * Для чего:
     * - Логи/метрики/вывод пользователю в одном месте.
     * <p>
     * Пример:
     * <pre>{@code
     * String now = UtilDate.now("uuuu-MM-dd'T'HH:mm:ss", UtilDate.DEFAULT_ZONE);
     * }</pre>
     */
    public static String now(String pattern, ZoneId zoneId) {
        return format(Instant.now(), pattern, zoneId);
    }

    /* ----------------------------- PARSE --------------------------- */

    /**
     * Универсально парсит {@code text} по {@code pattern} и возвращает {@link Instant}.
     * <p>
     * Правила интерпретации:
     * 1) Если текст/паттерн содержит zone (например [Europe/Moscow]) -> парсим как {@link ZonedDateTime} -> Instant.
     * 2) Если содержит offset (например Z или +03:00) -> парсим как {@link OffsetDateTime} -> Instant.
     * 3) Если содержит date+time, но без zone/offset -> считаем это локальным временем в {@code zoneId}.
     * 4) Если содержит только date -> считаем начало дня (00:00:00) в {@code zoneId}.
     * 5) Если содержит только time -> это НЕ момент времени (нет даты) -> исключение.
     * <p>
     * Для чего:
     * - Один предсказуемый метод вместо набора parseDate/parseLocalDateTime.
     * <p>
     * Примеры:
     * <pre>{@code
     * // Только дата -> начало дня в DEFAULT_ZONE
     * Instant a = UtilDate.parseToInstant("2025-01-19", "uuuu-MM-dd", UtilDate.DEFAULT_ZONE);
     *
     * // Дата-время без зоны -> интерпретируем как локальное время в DEFAULT_ZONE
     * Instant b = UtilDate.parseToInstant("2025-01-19T10:15:00", "uuuu-MM-dd'T'HH:mm:ss", UtilDate.DEFAULT_ZONE);
     *
     * // Дата-время с offset -> абсолютный момент
     * Instant c = UtilDate.parseToInstant("2025-01-19T10:15:00+03:00", "uuuu-MM-dd'T'HH:mm:ssXXX", UtilDate.DEFAULT_ZONE);
     *
     * // Дата-время с zone -> абсолютный момент
     * Instant d = UtilDate.parseToInstant("2025-01-19T10:15:00+03:00[Europe/Moscow]",
     *     "uuuu-MM-dd'T'HH:mm:ssXXX'['VV']'", UtilDate.DEFAULT_ZONE);
     * }</pre>
     */
    public static Instant parseToInstant(String text, String pattern, ZoneId zoneId) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(pattern, "pattern");
        Objects.requireNonNull(zoneId, "zoneId");

        TemporalAccessor ta;
        try {
            ta = formatter(pattern).parse(text);
        } catch (DateTimeParseException e) {
            throw new DateTimeParseException(
                    "Cannot parse: text='" + text + "', pattern='" + pattern + "'. " + e.getMessage(),
                    text,
                    e.getErrorIndex(),
                    e
            );
        }

        ZoneId parsedZone = ta.query(TemporalQueries.zone());
        ZoneOffset parsedOffset = ta.query(TemporalQueries.offset());

        try {
            if (parsedZone != null) {
                return ZonedDateTime.from(ta).toInstant();
            }
            if (parsedOffset != null) {
                return OffsetDateTime.from(ta).toInstant();
            }

            LocalDate date = ta.query(TemporalQueries.localDate());
            LocalTime time = ta.query(TemporalQueries.localTime());

            if (date != null && time != null) {
                return LocalDateTime.of(date, time).atZone(zoneId).toInstant();
            }
            if (date != null) {
                return date.atStartOfDay(zoneId).toInstant();
            }
            if (time != null) {
                throw new DateTimeParseException(
                        "Parsed time without date; cannot convert to Instant: text='" + text + "', pattern='" + pattern + "'",
                        text,
                        0
                );
            }

            return Instant.from(ta);

        } catch (DateTimeException e) {
            throw new DateTimeParseException(
                    "Parsed value cannot be converted to Instant: text='" + text + "', pattern='" + pattern + "'. " + e.getMessage(),
                    text,
                    0,
                    e
            );
        }
    }

    /**
     * Универсально парсит {@code text} по {@code pattern} и возвращает epochMillis.
     * <p>
     * Для чего:
     * - Когда нужно получить миллисекунды напрямую (для БД/кэша/сериализации).
     * <p>
     * Пример:
     * <pre>{@code
     * long ms = UtilDate.parseToEpochMilli("2025-01-19", "uuuu-MM-dd");
     * }</pre>
     */
    public static long parseToEpochMilli(String text, String pattern, ZoneId zoneId) {
        return parseToInstant(text, pattern, zoneId).toEpochMilli();
    }

    /**
     * Универсально парсит {@code text} по {@code pattern} и возвращает epochSeconds.
     * <p>
     * Для чего:
     * - Когда интеграция/контракт ожидает seconds.
     * <p>
     * Пример:
     * <pre>{@code
     * long sec = UtilDate.parseToEpochSecond("2025-01-19T10:15:00", "uuuu-MM-dd'T'HH:mm:ss");
     * }</pre>
     */
    public static long parseToEpochSecond(String text, String pattern, ZoneId zoneId) {
        return parseToInstant(text, pattern, zoneId).getEpochSecond();
    }

    /**
     * Парсит ISO-8601 строку, где offset/zone зашиты в тексте.
     * <p>
     * Для чего:
     * - Когда вы получаете дату из внешних систем в ISO-формате (обычно это лучший вариант обмена).
     * <p>
     * Поддерживаемые примеры:
     * - "2025-01-19T07:15:00Z"
     * - "2025-01-19T10:15:00+03:00"
     * - "2025-01-19T10:15:00+03:00[Europe/Moscow]"
     * <p>
     * Пример:
     * <pre>{@code
     * Instant t1 = UtilDate.parseIso("2025-01-19T07:15:00Z");
     * Instant t2 = UtilDate.parseIso("2025-01-19T10:15:00+03:00");
     * }</pre>
     */
    public static Instant parseIso(String text) {
        Objects.requireNonNull(text, "text");
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException ignored) {
            try {
                return OffsetDateTime.parse(text).toInstant();
            } catch (DateTimeParseException ignored2) {
                return ZonedDateTime.parse(text).toInstant();
            }
        }
    }

    /* ---------------------------- VALIDATE ------------------------- */

    /**
     * Строго валидирует, что {@code text} парсится по {@code pattern} в момент времени (Instant)
     * с правилами {@link #parseToInstant(String, String, ZoneId)}.
     * <p>
     * Для чего:
     * - Проверка пользовательского ввода или данных конфигурации.
     * <p>
     * Пример:
     * <pre>{@code
     * boolean ok1 = UtilDate.validate("2025-01-19", "uuuu-MM-dd");
     * boolean ok2 = UtilDate.validate("2025-02-31", "uuuu-MM-dd"); // false
     * }</pre>
     */
    public static boolean validate(String text, String pattern, ZoneId zoneId) {
        try {
            parseToInstant(text, pattern, zoneId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /* ----------------------------- OFFSET -------------------------- */

    /**
     * Возвращает смещение (offset) в секундах для {@code zoneId} на конкретный момент {@code instant}.
     * <p>
     * Для чего:
     * - Если зона поддерживает DST, offset может меняться во времени, и его нужно вычислять "на момент".
     * <p>
     * Пример:
     * <pre>{@code
     * int offset = UtilDate.offsetSecondsAt(ZoneId.of("Europe/Amsterdam"), Instant.now());
     * }</pre>
     */
    public static int offsetSecondsAt(ZoneId zoneId, Instant instant) {
        Objects.requireNonNull(zoneId, "zoneId");
        Objects.requireNonNull(instant, "instant");
        return zoneId.getRules().getOffset(instant).getTotalSeconds();
    }

    /**
     * Возвращает текущий offset (в секундах) для {@code zoneId}.
     * <p>
     * Для чего:
     * - Быстрый доступ к текущему смещению (важно: для DST-зон будет зависеть от даты).
     * <p>
     * Пример:
     * <pre>{@code
     * int msk = UtilDate.currentOffsetSeconds(UtilDate.DEFAULT_ZONE); // обычно 10800
     * }</pre>
     */
    public static int currentOffsetSeconds(ZoneId zoneId) {
        return offsetSecondsAt(zoneId, Instant.now());
    }

}
