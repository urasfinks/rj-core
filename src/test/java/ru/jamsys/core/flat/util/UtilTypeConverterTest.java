package ru.jamsys.core.flat.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class UtilTypeConverterTest {

    @Test
    void testToBigDecimal_validCases() {
        assertEquals(0, Objects.requireNonNull(UtilTypeConverter.toBigDecimal("123.45")).compareTo(new BigDecimal("123.45")));
        assertEquals(0, Objects.requireNonNull(UtilTypeConverter.toBigDecimal(100)).compareTo(new BigDecimal("100")));
        assertEquals(0, Objects.requireNonNull(UtilTypeConverter.toBigDecimal(42.0)).compareTo(new BigDecimal("42.0")));
        assertEquals(0, Objects.requireNonNull(UtilTypeConverter.toBigDecimal(123)).compareTo(new BigDecimal("123")));
        assertEquals(0, Objects.requireNonNull(UtilTypeConverter.toBigDecimal(456789123456789L)).compareTo(new BigDecimal("456789123456789")));
        assertEquals(0, Objects.requireNonNull(UtilTypeConverter.toBigDecimal("-12.34")).compareTo(new BigDecimal("-12.34")));
        assertNull(UtilTypeConverter.toBigDecimal(null));
    }

    @Test
    void testToBigDecimal_invalidCases() {
        assertThrows(IllegalArgumentException.class, () -> UtilTypeConverter.toBigDecimal(""));
        assertThrows(IllegalArgumentException.class, () -> UtilTypeConverter.toBigDecimal("not a number"));
    }

    @Test
    void testToBoolean_validCases() {
        assertEquals(Boolean.TRUE, UtilTypeConverter.toBoolean(true));
        assertEquals(Boolean.FALSE, UtilTypeConverter.toBoolean(false));
        assertEquals(Boolean.TRUE, UtilTypeConverter.toBoolean(1));
        assertEquals(Boolean.FALSE, UtilTypeConverter.toBoolean(0));
        assertEquals(Boolean.TRUE, UtilTypeConverter.toBoolean(-1));
        assertEquals(Boolean.TRUE, UtilTypeConverter.toBoolean("yes"));
        assertEquals(Boolean.FALSE, UtilTypeConverter.toBoolean("no"));
        assertEquals(Boolean.TRUE, UtilTypeConverter.toBoolean("TRUE"));
        assertEquals(Boolean.FALSE, UtilTypeConverter.toBoolean("false"));
        assertEquals(Boolean.TRUE, UtilTypeConverter.toBoolean("1"));
        assertEquals(Boolean.FALSE, UtilTypeConverter.toBoolean("0"));
        assertEquals(Boolean.TRUE, UtilTypeConverter.toBoolean("  YES "));
        assertEquals(Boolean.FALSE, UtilTypeConverter.toBoolean(" No "));
        assertNull(UtilTypeConverter.toBoolean(null));
    }

    @Test
    void testToBoolean_invalidCases() {
        assertThrows(IllegalArgumentException.class, () -> UtilTypeConverter.toBoolean("maybe"));
        assertThrows(IllegalArgumentException.class, () -> UtilTypeConverter.toBoolean(""));
    }

    @Test
    void testToTimestamp_validCases() {
        long now = System.currentTimeMillis();
        assertEquals(new Timestamp(now), UtilTypeConverter.toTimestamp(now));
        assertEquals(new Timestamp(now), UtilTypeConverter.toTimestamp(Long.toString(now)));

        ZonedDateTime zdt = ZonedDateTime.now();
        assertEquals(Timestamp.valueOf(zdt.toLocalDateTime()), UtilTypeConverter.toTimestamp(zdt.toString()));

        LocalDateTime ldt = LocalDateTime.now();
        assertEquals(Timestamp.valueOf(ldt), UtilTypeConverter.toTimestamp(ldt));

        ZonedDateTime zdt2 = ZonedDateTime.now();
        assertEquals(Timestamp.valueOf(zdt2.toLocalDateTime()), UtilTypeConverter.toTimestamp(zdt2));

        Instant inst = Instant.now();
        assertEquals(Timestamp.from(inst), UtilTypeConverter.toTimestamp(inst));

        assertNull(UtilTypeConverter.toTimestamp(null));
    }

    @Test
    void testToTimestamp_invalidCases() {
        assertThrows(IllegalArgumentException.class, () -> UtilTypeConverter.toTimestamp("not-a-date"));
        assertThrows(IllegalArgumentException.class, () -> UtilTypeConverter.toTimestamp(""));
        assertThrows(IllegalArgumentException.class, () -> UtilTypeConverter.toTimestamp("2025-13-01T00:00:00Z")); // invalid month
    }

}