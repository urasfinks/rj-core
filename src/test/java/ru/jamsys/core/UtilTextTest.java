package ru.jamsys.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilText;

import java.util.function.Function;

// IO time: 13ms
// COMPUTE time: 10ms

class UtilTextTest {

    @Test
    void firstCharToUpperCase() {
        Assertions.assertEquals("HelloWorld", UtilText.firstCharToUpperCase("HelloWorld"));
        Assertions.assertEquals("HelloWorld", UtilText.firstCharToUpperCase("helloWorld"));
        Assertions.assertEquals("", UtilText.firstCharToUpperCase(""));
        Assertions.assertEquals("U", UtilText.firstCharToUpperCase("u"));
        Assertions.assertEquals("Ur", UtilText.firstCharToUpperCase("ur"));
        Assertions.assertEquals("U", UtilText.firstCharToUpperCase("U"));
        Assertions.assertEquals("UR", UtilText.firstCharToUpperCase("UR"));
        Assertions.assertEquals("URa", UtilText.firstCharToUpperCase("URa"));
        Assertions.assertNull(UtilText.firstCharToUpperCase(null));
    }

    @Test
    void firstCharToLowerCase() {
        Assertions.assertEquals("helloWorld", UtilText.firstCharToLowerCase("HelloWorld"));
        Assertions.assertEquals("helloWorld", UtilText.firstCharToLowerCase("helloWorld"));
        Assertions.assertEquals("", UtilText.firstCharToLowerCase(""));
        Assertions.assertEquals("u", UtilText.firstCharToLowerCase("U"));
        Assertions.assertEquals("ur", UtilText.firstCharToLowerCase("Ur"));
        Assertions.assertEquals("u", UtilText.firstCharToLowerCase("u"));
        Assertions.assertEquals("ur", UtilText.firstCharToLowerCase("ur"));
        Assertions.assertEquals("urA", UtilText.firstCharToLowerCase("urA"));
        Assertions.assertNull(UtilText.firstCharToLowerCase(null));
    }

    @Test
    void readUntil() {
        Assertions.assertEquals("00", UtilText.readUntil("00p", Util::isNumeric));
    }

    @Test
    void digitTranslate() {
        Assertions.assertEquals("коров", UtilText.digitTranslate(5, "корова", "коровы", "коров"));
        Assertions.assertEquals("коров", UtilText.digitTranslate(25, "корова", "коровы", "коров"));
        Assertions.assertEquals("коров", UtilText.digitTranslate(35, "корова", "коровы", "коров"));

        Assertions.assertEquals("корова", UtilText.digitTranslate(1, "корова", "коровы", "коров"));
        Assertions.assertEquals("коровы", UtilText.digitTranslate(2, "корова", "коровы", "коров"));
        Assertions.assertEquals("коровы", UtilText.digitTranslate(3, "корова", "коровы", "коров"));
        Assertions.assertEquals("коровы", UtilText.digitTranslate(4, "корова", "коровы", "коров"));
        Assertions.assertEquals("коров", UtilText.digitTranslate(6, "корова", "коровы", "коров"));
        Assertions.assertEquals("коров", UtilText.digitTranslate(7, "корова", "коровы", "коров"));
        Assertions.assertEquals("коров", UtilText.digitTranslate(8, "корова", "коровы", "коров"));
        Assertions.assertEquals("коров", UtilText.digitTranslate(9, "корова", "коровы", "коров"));
        Assertions.assertEquals("коров", UtilText.digitTranslate(10, "корова", "коровы", "коров"));
        Assertions.assertEquals("коров", UtilText.digitTranslate(11, "корова", "коровы", "коров"));
        Assertions.assertEquals("коров", UtilText.digitTranslate(12, "корова", "коровы", "коров"));
        Assertions.assertEquals("коров", UtilText.digitTranslate(13, "корова", "коровы", "коров"));
        Assertions.assertEquals("коров", UtilText.digitTranslate(14, "корова", "коровы", "коров"));
        Assertions.assertEquals("коров", UtilText.digitTranslate(15, "корова", "коровы", "коров"));
        Assertions.assertEquals("коров", UtilText.digitTranslate(16, "корова", "коровы", "коров"));
        Assertions.assertEquals("коров", UtilText.digitTranslate(17, "корова", "коровы", "коров"));
        Assertions.assertEquals("коров", UtilText.digitTranslate(18, "корова", "коровы", "коров"));
        Assertions.assertEquals("коров", UtilText.digitTranslate(19, "корова", "коровы", "коров"));
        Assertions.assertEquals("коров", UtilText.digitTranslate(20, "корова", "коровы", "коров"));
        Assertions.assertEquals("корова", UtilText.digitTranslate(21, "корова", "коровы", "коров"));
    }

    @Test
    void testPadRightDefault() {
        Assertions.assertEquals("abc     ", UtilText.padRight("abc", 8));
        Assertions.assertEquals("abc", UtilText.padRight("abc", 3));
        Assertions.assertEquals("", UtilText.padRight(null, 5));
    }

    @Test
    void testPadRightCustomChar() {
        Assertions.assertEquals("abc***", UtilText.padRight("abc", 6, "*"));
        Assertions.assertEquals("abc", UtilText.padRight("abc", 3, "*"));
        Assertions.assertEquals("abc   ", UtilText.padRight("abc", 6, null));
        Assertions.assertEquals("", UtilText.padRight(null, 5, "*"));
    }

    @Test
    void testPadLeftDefault() {
        Assertions.assertEquals("     abc", UtilText.padLeft("abc", 8));
        Assertions.assertEquals("abc", UtilText.padLeft("abc", 3));
        Assertions.assertEquals("   null", UtilText.padLeft("null", 7)); // formatted string with default space
    }

    @Test
    void testPadLeftCustomChar() {
        Assertions.assertEquals("***abc", UtilText.padLeft("abc", 6, "*"));
        Assertions.assertEquals("abc", UtilText.padLeft("abc", 3, "*"));
        Assertions.assertEquals("   abc", UtilText.padLeft("abc", 6, null));
        Assertions.assertEquals("", UtilText.padLeft(null, 5, "*"));
    }

    @Test
    void testTrimLeft() {
        Assertions.assertEquals("abc", UtilText.trimLeft("***abc", "*"));
        Assertions.assertEquals("abc", UtilText.trimLeft("abc", "*"));
        Assertions.assertEquals("", UtilText.trimLeft("****", "*"));
    }

    @Test
    void testTrimRight() {
        Assertions.assertEquals("abc", UtilText.trimRight("abc***", "*"));
        Assertions.assertEquals("abc", UtilText.trimRight("abc", "*"));
        Assertions.assertEquals("", UtilText.trimRight("****", "*"));
    }

    @SuppressWarnings("all")
    @Test
    void testCapitalize() {
        Assertions.assertEquals("Abc", UtilText.capitalize("abc"));
        Assertions.assertEquals("A", UtilText.capitalize("a"));
        Assertions.assertEquals("", UtilText.capitalize(""));
        Assertions.assertNull(UtilText.capitalize(null));
    }

    @Test
    void testRegexpReplace() {
        Assertions.assertEquals("111", UtilText.regexpReplace("abc", ".", "1"));
        Assertions.assertEquals("a1c", UtilText.regexpReplace("abc", "b", "1"));
        Assertions.assertEquals("abc", UtilText.regexpReplace("abc", "z", "1")); // no match
    }

    @Test
    void testRegexpFind() {
        Assertions.assertEquals("b", UtilText.regexpFind("abc", "b"));
        Assertions.assertNull(UtilText.regexpFind("abc", "z"));
    }

    @Test
    void testFirstCharToLowerCase() {
        Assertions.assertEquals("abc", UtilText.firstCharToLowerCase("Abc"));
        Assertions.assertEquals("a", UtilText.firstCharToLowerCase("A"));
        Assertions.assertEquals("", UtilText.firstCharToLowerCase(""));
        Assertions.assertNull(UtilText.firstCharToLowerCase(null));
    }

    @Test
    void testFirstCharToUpperCase() {
        Assertions.assertEquals("Abc", UtilText.firstCharToUpperCase("abc"));
        Assertions.assertEquals("A", UtilText.firstCharToUpperCase("a"));
        Assertions.assertEquals("", UtilText.firstCharToUpperCase(""));
        Assertions.assertNull(UtilText.firstCharToUpperCase(null));
    }

    @Test
    void testDigitTranslateInt() {
        Assertions.assertEquals("яблоко", UtilText.digitTranslate(1, "яблоко", "яблока", "яблок"));
        Assertions.assertEquals("яблока", UtilText.digitTranslate(3, "яблоко", "яблока", "яблок"));
        Assertions.assertEquals("яблок", UtilText.digitTranslate(5, "яблоко", "яблока", "яблок"));
        Assertions.assertEquals("яблок", UtilText.digitTranslate(11, "яблоко", "яблока", "яблок"));
    }

    @Test
    void testDigitTranslateLong() {
        Assertions.assertEquals("монета", UtilText.digitTranslate(1L, "монета", "монеты", "монет"));
        Assertions.assertEquals("монеты", UtilText.digitTranslate(24L, "монета", "монеты", "монет"));
        Assertions.assertEquals("монет", UtilText.digitTranslate(100L, "монета", "монеты", "монет"));
        Assertions.assertEquals("монет", UtilText.digitTranslate(112L, "монета", "монеты", "монет"));
    }

    @Test
    void testReadUntil() {
        Function<String, Boolean> isDigit = s -> s.matches("\\d");
        Assertions.assertEquals("123", UtilText.readUntil("123abc", isDigit));
        Assertions.assertEquals("", UtilText.readUntil("abc", isDigit));
        Assertions.assertEquals("1", UtilText.readUntil("1abc2", isDigit));
    }

}