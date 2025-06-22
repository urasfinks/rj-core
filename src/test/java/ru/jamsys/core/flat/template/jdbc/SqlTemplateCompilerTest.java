package ru.jamsys.core.flat.template.jdbc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.*;

class SqlTemplateCompilerTest {

    @Test
    void staticSqlWithNonNullArgument() throws CloneNotSupportedException {
        SqlTemplateCompiler compiler = new SqlTemplateCompiler(
                "SELECT * FROM table WHERE name = ${IN.name::VARCHAR}"
        );

        Map<String, Object> args = Map.of("name", "John");

        SqlTemplateCompiled result = compiler.compile(args);

        Assertions.assertEquals("SELECT * FROM table WHERE name = ?", result.getSql());
        Assertions.assertEquals(1, result.getListArgument().size());

        Argument arg = result.getListArgument().getFirst();
        Assertions.assertEquals("John", arg.getValue());
        Assertions.assertEquals("name", arg.getKey());
        Assertions.assertEquals(1, arg.getIndex());
    }

    @Test
    void staticSqlWithNullArgument() throws CloneNotSupportedException {
        SqlTemplateCompiler compiler = new SqlTemplateCompiler(
                "SELECT * FROM table WHERE name = ${IN.name::VARCHAR}"
        );

        Map<String, Object> args = new HashMap<>();
        args.put("name", null);

        SqlTemplateCompiled result = compiler.compile(args);

        Assertions.assertEquals("SELECT * FROM table WHERE name = ?", result.getSql());
        Assertions.assertEquals(1, result.getListArgument().size());
        Assertions.assertNull(result.getListArgument().getFirst().getValue());
    }

    @Test
    void dynamicInClauseWithList() throws CloneNotSupportedException {
        SqlTemplateCompiler compiler = new SqlTemplateCompiler(
                "SELECT * FROM table WHERE id IN (${IN.ids::IN_ENUM_NUMBER})"
        );

        Map<String, Object> args = new HashMap<>();
        args.put("ids", Arrays.asList(1, 2, 3));

        SqlTemplateCompiled result = compiler.compile(args);

        Assertions.assertEquals("SELECT * FROM table WHERE id IN (?,?,?)", result.getSql());
        Assertions.assertEquals(3, result.getListArgument().size());

        for (int i = 0; i < 3; i++) {
            Argument arg = result.getListArgument().get(i);
            Assertions.assertEquals(i + 1, arg.getIndex());
            Assertions.assertEquals(1 + i, arg.getValue());
        }
    }

    @Test
    void dynamicInClauseWithNullList() throws CloneNotSupportedException {
        SqlTemplateCompiler compiler = new SqlTemplateCompiler(
                "SELECT * FROM table WHERE id IN (${IN.ids::IN_ENUM_NUMBER})"
        );

        Map<String, Object> args = new HashMap<>();
        args.put("ids", null);

        SqlTemplateCompiled result = compiler.compile(args);

        Assertions.assertEquals("SELECT * FROM table WHERE id IN ()", result.getSql());
        Assertions.assertTrue(result.getListArgument().isEmpty());
    }

    @Test
    void dynamicInClauseWithNull() throws CloneNotSupportedException {
        SqlTemplateCompiler compiler = new SqlTemplateCompiler(
                "SELECT * FROM table WHERE id IN (${IN.ids::IN_ENUM_NUMBER})"
        );

        Map<String, Object> args = new HashMap<>();
        args.put("ids", null);

        SqlTemplateCompiled result = compiler.compile(args);

        Assertions.assertEquals("SELECT * FROM table WHERE id IN ()", result.getSql());
        Assertions.assertEquals(0, result.getListArgument().size());
    }

    @Test
    void multipleArgumentsWithReuse() throws CloneNotSupportedException {
        SqlTemplateCompiler compiler = new SqlTemplateCompiler(
                "SELECT * FROM table WHERE name = ${IN.name::VARCHAR} OR nickname = ${IN.name::VARCHAR}"
        );

        Map<String, Object> args = Map.of("name", "Alex");

        SqlTemplateCompiled result = compiler.compile(args);

        Assertions.assertEquals("SELECT * FROM table WHERE name = ? OR nickname = ?", result.getSql());
        Assertions.assertEquals(2, result.getListArgument().size());

        for (Argument arg : result.getListArgument()) {
            Assertions.assertEquals("Alex", arg.getValue());
        }
    }

}