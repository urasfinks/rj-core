package ru.jamsys.cache;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.util.Util;

class MapExpiredTest {

    @Test
    void add() {
        MapExpired<Integer, String> mapExpired = new MapExpired<>();

        mapExpired.add(1234, "Hello world", 100);
        Assertions.assertEquals(1, mapExpired.get().size(), "#1");

        mapExpired.add(12345, "Hello world", 100);
        Assertions.assertEquals(2, mapExpired.get().size(), "#2");

        Assertions.assertFalse(mapExpired.add(12345, "Hello world", 100), "#3");

        mapExpired.add(123456, "Hello world", 1000);
        Assertions.assertEquals(3, mapExpired.get().size(), "#4");

        Util.sleepMs(200);
        mapExpired.keepAlive(null);

        mapExpired.add(1234567, "Hello world", 100);
        Assertions.assertEquals(2, mapExpired.get().size(), "#5");
    }
}