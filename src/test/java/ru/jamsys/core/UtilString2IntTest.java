package ru.jamsys.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.Util;

import java.util.HashSet;
import java.util.Set;

// IO time: 13ms
// COMPUTE time: 10ms

class UtilString2IntTest {

    @Test
    void testNullInputReturnsMin() {
        int result = Util.stringToInt(null, 10, 20);
        Assertions.assertEquals(10, result);
    }

    @Test
    void testEmptyStringIsInRange() {
        int result = Util.stringToInt("", 0, 5);
        Assertions.assertTrue(result >= 0 && result <= 5);
    }

    @Test
    void testConsistentHashing() {
        int a = Util.stringToInt("hello", 100, 200);
        int b = Util.stringToInt("hello", 100, 200);
        Assertions.assertEquals(a, b);
    }

    @Test
    void testDifferentInputsProduceDifferentResults() {
        int a = Util.stringToInt("foo", 0, 1000);
        int b = Util.stringToInt("bar", 0, 1000);
        Assertions.assertNotEquals(a, b); // допускается коллизия, но маловероятна
    }

    @Test
    void testOutputAlwaysInRange() {
        for (int i = 0; i < 100; i++) {
            String input = "test" + i;
            int result = Util.stringToInt(input, 10, 50);
            Assertions.assertTrue(result >= 10 && result <= 50, "Result out of range: " + result);
        }
    }

    @Test
    void testMinEqualsMaxAlwaysReturnsMin() {
        int result = Util.stringToInt("any", 7, 7);
        Assertions.assertEquals(7, result);
    }

    @Test
    void testMinGreaterThanMaxReturnsMin() {
        int result = Util.stringToInt("value", 20, 10);
        Assertions.assertEquals(20, result);
    }

    @Test
    void testSpreadDistribution() {
        int min = 0, max = 10;
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            int value = Util.stringToInt("key" + i, min, max);
            seen.add(value);
        }
        // хотя бы 8 разных значений из 11 возможных — приемлемо
        Assertions.assertTrue(seen.size() >= 8, "Distribution not varied enough: " + seen);
    }

}