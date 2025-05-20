package ru.jamsys.core.flat.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

class UtilFileTest {

    @Test
    void test1() {
        String string = Paths.get("LogManager/test2.afwr").toAbsolutePath().toString();
        Assertions.assertEquals(
                "LogManager/test2.afwr",
                UtilFile.getRelativePath("LogManager", string)
        );
    }

}