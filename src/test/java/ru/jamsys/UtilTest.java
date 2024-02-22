package ru.jamsys;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UtilTest {

    @Test
    void snakeToCamel() {
        Assertions.assertEquals("HelloWorld", Util.snakeToCamel("HELLO_WORLD"));
        Assertions.assertEquals("HelloWorld1", Util.snakeToCamel("HELLO_WORLD1"));
        Assertions.assertEquals("HelloWorld1", Util.snakeToCamel("HELLO_WORLD_1"));
        Assertions.assertEquals("Helloworld", Util.snakeToCamel("HELLOWORLD"));
    }
}