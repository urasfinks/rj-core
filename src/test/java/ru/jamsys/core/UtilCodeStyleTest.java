package ru.jamsys.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

// IO time: 13ms
// COMPUTE time: 10ms

class UtilCodeStyleTest {

    @Test
    void snakeToCamel() {
        Assertions.assertEquals("HelloWorld", ru.jamsys.core.flat.UtilCodeStyle.snakeToCamel("HELLO_WORLD"));
        Assertions.assertEquals("HelloWorld1", ru.jamsys.core.flat.UtilCodeStyle.snakeToCamel("HELLO_WORLD1"));
        Assertions.assertEquals("HelloWorld1", ru.jamsys.core.flat.UtilCodeStyle.snakeToCamel("HELLO_WORLD_1"));
        Assertions.assertEquals("HelloWorld1_", ru.jamsys.core.flat.UtilCodeStyle.snakeToCamel("HELLO_WORLD_1_"));
        Assertions.assertEquals("_HelloWorld1_", ru.jamsys.core.flat.UtilCodeStyle.snakeToCamel("_HELLO_WORLD_1_"));
        Assertions.assertEquals("Helloworld", ru.jamsys.core.flat.UtilCodeStyle.snakeToCamel("HELLOWORLD"));
        Assertions.assertEquals("DataType", ru.jamsys.core.flat.UtilCodeStyle.snakeToCamel("dataType"));
        Assertions.assertEquals("DataType", ru.jamsys.core.flat.UtilCodeStyle.snakeToCamel("DataType"));
        Assertions.assertEquals("DataType", ru.jamsys.core.flat.UtilCodeStyle.snakeToCamel("data_Type"));
        Assertions.assertEquals("DataType", ru.jamsys.core.flat.UtilCodeStyle.snakeToCamel("data_type"));
    }

    @Test
    void camelToSnake() {
        Assertions.assertEquals("HELLO_WORLD", ru.jamsys.core.flat.UtilCodeStyle.camelToSnake("HelloWorld"));
        Assertions.assertEquals("_HELLOWORLD", ru.jamsys.core.flat.UtilCodeStyle.camelToSnake("_HelloWorld"));
        Assertions.assertEquals("HELLO_WORLD1", ru.jamsys.core.flat.UtilCodeStyle.camelToSnake("HelloWorld1"));
        Assertions.assertEquals("HELLOWORLD", ru.jamsys.core.flat.UtilCodeStyle.camelToSnake("Helloworld"));
        Assertions.assertEquals("HELLO_WORLD", ru.jamsys.core.flat.UtilCodeStyle.camelToSnake("Hello_world"));
    }

}