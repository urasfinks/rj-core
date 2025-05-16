package ru.jamsys.core.flat.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UtilFileTest {

    @Test
    void test1() {
        Assertions.assertEquals(
                "LogManager/test2.afwr",
                UtilFile.getRelativePath("LogManager", "/Users/sfinks/IdeaProjects/last-core/rj-core/LogManager/test2.afwr")
        );
    }

}