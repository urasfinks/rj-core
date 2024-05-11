package ru.jamsys.core.cache;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.component.item.Session;
import ru.jamsys.core.util.Util;

import java.util.concurrent.atomic.AtomicBoolean;

class SessionTest {

    AtomicBoolean isThreadRun = new AtomicBoolean(true);
    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        App.main(args);
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @Test
    void add() {
        Session<Integer, String> cache = new Session<>("test");

        cache.add(1234, "Hello world", 100);
        Assertions.assertEquals(1, cache.getMap().size(), "#1");

        cache.add(12345, "Hello world", 100);
        Assertions.assertEquals(2, cache.getMap().size(), "#2");

        cache.add(123456, "Hello world", 1000);
        Assertions.assertEquals(3, cache.getMap().size(), "#4");

        Util.sleepMs(200);

        cache.keepAlive(isThreadRun);

        cache.add(1234567, "Hello world", 100);
        Assertions.assertEquals(2, cache.getMap().size(), "#5");
    }

}