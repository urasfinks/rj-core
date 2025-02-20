package ru.jamsys.core.component.manager.item;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.ManagerSession;
import ru.jamsys.core.flat.util.Util;

class SessionTest {

    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runCore();
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    public static class XTest {

    }

    @Test
    void computeIfAbsent() {
        Session<Integer, XTest> test = App.get(ManagerSession.class).get("test", 1000);
        XTest s = test.computeIfAbsent(10, _ -> new XTest());
        Assertions.assertEquals(1, test.size());
        Assertions.assertEquals(1, test.sizeExpiration());
        Assertions.assertEquals(s.hashCode(), test.get(10).hashCode());
        Assertions.assertEquals(s.hashCode(), test.get(10).hashCode());
        Util.sleepMs(2000);
        Assertions.assertEquals(0, test.size());
        Assertions.assertEquals(0, test.sizeExpiration());
    }
}