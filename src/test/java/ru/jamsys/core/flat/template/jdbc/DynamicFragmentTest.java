package ru.jamsys.core.flat.template.jdbc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.extension.exception.ForwardException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class DynamicFragmentTest {

    @Test
    void compileInEnumNumber_shouldGeneratePlaceholders() {
        List<Integer> values = Arrays.asList(1, 2, 3);
        String sql = DynamicFragment.compile(ArgumentType.IN_ENUM_NUMBER, values);

        Assertions.assertEquals("?,?,?", sql);
    }

    @Test
    void compileInEnumVarchar_shouldGeneratePlaceholders() {
        List<String> values = Arrays.asList("a", "b");
        String sql = DynamicFragment.compile(ArgumentType.IN_ENUM_VARCHAR, values);

        Assertions.assertEquals("?,?", sql);
    }

    @Test
    void compileEmptyList_shouldThrowException() {
        List<Object> emptyList = Collections.emptyList();

        RuntimeException ex = Assertions.assertThrows(RuntimeException.class, () ->
                DynamicFragment.compile(ArgumentType.IN_ENUM_NUMBER, emptyList)
        );

        Assertions.assertTrue(ex.getMessage().contains("list is empty"));
    }

    @Test
    void compileWithNull_shouldThrowException() {
        RuntimeException ex = Assertions.assertThrows(RuntimeException.class, () ->
                DynamicFragment.compile(ArgumentType.IN_ENUM_NUMBER, null)
        );
        Assertions.assertTrue(ex.getMessage().contains("expects List"));
    }

    @Test
    void compileWithInvalidType_shouldThrowException() {
        RuntimeException ex = Assertions.assertThrows(RuntimeException.class, () ->
                DynamicFragment.compile(ArgumentType.IN_ENUM_NUMBER, "not-a-list")
        );

        Assertions.assertTrue(ex.getMessage().contains("expects List"));
    }

    @Test
    void unsupportedArgumentType_shouldThrowForwardException() {
        ForwardException ex = Assertions.assertThrows(ForwardException.class, () ->
                DynamicFragment.compile(ArgumentType.VARCHAR, List.of("x"))
        );
        Assertions.assertTrue(ex.getMessage().contains("Unsupported dynamic argument type"));
    }

    @Test
    void checkShouldReturnTrueForDynamicTypes() {
        Assertions.assertTrue(DynamicFragment.check(ArgumentType.IN_ENUM_VARCHAR));
        Assertions.assertTrue(DynamicFragment.check(ArgumentType.IN_ENUM_TIMESTAMP));
        Assertions.assertTrue(DynamicFragment.check(ArgumentType.IN_ENUM_NUMBER));
    }

    @Test
    void checkShouldReturnFalseForStaticTypes() {
        Assertions.assertFalse(DynamicFragment.check(ArgumentType.VARCHAR));
        Assertions.assertFalse(DynamicFragment.check(ArgumentType.TIMESTAMP));
        Assertions.assertFalse(DynamicFragment.check(ArgumentType.BOOLEAN));
    }

}