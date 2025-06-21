package ru.jamsys.core.flat.template.jdbc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ArgumentTest {

    @Test
    void testValidTemplateParsing() {
        String template = "IN.userId::NUMBER";
        Argument arg = Argument.getInstance(template);

        assertEquals(ArgumentDirection.IN, arg.getDirection());
        assertEquals(ArgumentType.NUMBER, arg.getType());
        assertEquals("userId", arg.getKey());
        assertEquals(template, arg.getKeySqlTemplate());
    }

    @Test
    void testValidEnumTemplateParsing() {
        String template = "OUT.status::IN_ENUM_VARCHAR";
        Argument arg = Argument.getInstance(template);

        assertEquals(ArgumentDirection.OUT, arg.getDirection());
        assertEquals(ArgumentType.IN_ENUM_VARCHAR, arg.getType());
        assertEquals("status", arg.getKey());
    }

    @Test
    void testIN_OUTDirection() {
        String template = "IN_OUT.value::BOOLEAN";
        Argument arg = Argument.getInstance(template);

        assertEquals(ArgumentDirection.IN_OUT, arg.getDirection());
        assertEquals(ArgumentType.BOOLEAN, arg.getType());
        assertEquals("value", arg.getKey());
    }

    @Test
    void testTemplateMissingDirectionShouldThrow() {
        String template = "someKey::VARCHAR";
        Exception ex = assertThrows(IllegalArgumentException.class, () -> Argument.getInstance(template));
        assertTrue(ex.getMessage().contains("Недостаточное описание шаблона"));
    }

    @Test
    void testTemplateMissingTypeShouldThrow() {
        String template = "IN.someKey";
        Exception ex = assertThrows(IllegalArgumentException.class, () -> Argument.getInstance(template));
        assertTrue(ex.getMessage().contains("Недостаточное описание шаблона"));
    }

    @Test
    void testEmptyTemplateShouldThrow() {
        Exception ex = assertThrows(IllegalArgumentException.class, () -> Argument.getInstance(""));
        assertTrue(ex.getMessage().contains("Пустой шаблон"));
    }

    @Test
    void testNullTemplateShouldThrow() {
        Exception ex = assertThrows(IllegalArgumentException.class, () -> Argument.getInstance(null));
        assertTrue(ex.getMessage().contains("Пустой шаблон"));
    }

    @Test
    void testInvalidDirectionShouldThrow() {
        String template = "UNKNOWN.key::VARCHAR";
        Exception ex = assertThrows(IllegalArgumentException.class, () -> Argument.getInstance(template));
        assertTrue(ex.getMessage().contains("No enum constant"));
    }

    @Test
    void testInvalidTypeShouldThrow() {
        String template = "IN.key::UNKNOWN_TYPE";
        Exception ex = assertThrows(IllegalArgumentException.class, () -> Argument.getInstance(template));
        assertTrue(ex.getMessage().contains("No enum constant"));
    }

}