package ru.jamsys.core.flat.util;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

public class UtilTypeConverter {

    public static BigDecimal toBigDecimal(Object value) {
        return switch (value) {
            case null -> null;
            case BigDecimal bd -> bd;
            case Number num -> BigDecimal.valueOf(num.doubleValue());
            case String str when !str.isBlank() -> new BigDecimal(str);
            default -> throw new IllegalArgumentException("Invalid number value: " + value);
        };
    }

    public static Boolean toBoolean(Object value) {
        return switch (value) {
            case null -> null;
            case Boolean b -> b;
            case Number num -> num.intValue() != 0;
            case String str when !str.isBlank() -> {
                String lower = str.trim().toLowerCase();
                yield switch (lower) {
                    case "true", "1", "yes" -> true;
                    case "false", "0", "no" -> false;
                    default -> throw new IllegalArgumentException("Invalid boolean string value: " + value);
                };
            }
            default -> throw new IllegalArgumentException("Invalid boolean value: " + value);
        };
    }

    public static Timestamp toTimestamp(Object value) {
        return switch (value) {
            case null -> null;
            case Timestamp ts -> ts;
            case Instant inst -> Timestamp.from(inst);
            case LocalDateTime ldt -> Timestamp.valueOf(ldt);
            case ZonedDateTime zdt -> Timestamp.valueOf(zdt.toLocalDateTime());
            case Long l -> new Timestamp(l);
            case String str when !str.isBlank() -> {
                String trimmed = str.trim();
                try {
                    // Попытка как миллисекунды
                    long millis = Long.parseLong(trimmed);
                    yield new Timestamp(millis);
                } catch (NumberFormatException e) {
                    // Попытка как ISO-строка
                    try {
                        ZonedDateTime zdt = ZonedDateTime.parse(trimmed);
                        yield Timestamp.valueOf(zdt.toLocalDateTime());
                    } catch (DateTimeParseException e1) {
                        try {
                            LocalDateTime ldt = LocalDateTime.parse(trimmed);
                            yield Timestamp.valueOf(ldt);
                        } catch (DateTimeParseException e2) {
                            throw new IllegalArgumentException("Invalid timestamp string: " + value);
                        }
                    }
                }
            }
            default -> throw new IllegalArgumentException("Invalid timestamp value: " + value);
        };
    }

}
