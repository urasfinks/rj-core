package ru.jamsys.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.extension.property.MapRepository;

import java.util.HashMap;
import java.util.Map;

// IO time: 11ms
// COMPUTE time: 12ms

class TestMapRepositorySimple {

    public static class P implements MapRepository<String, Object> {

        Map<String, Object> map = new HashMap<>();

        @Override
        public Map<String, Object> getMapRepository() {
            return map;
        }
    }

    @Test
    void test() {
        P p = new P();

        Assertions.assertEquals("y", p.setToMapRepository("x", "y"));
        Assertions.assertEquals("y", p.setToMapRepository("x", "y2"));
        Assertions.assertEquals("y", p.getFromMapRepository("x", String.class, ""));
        Assertions.assertEquals("def", p.getFromMapRepository("x1", String.class, "def"));

    }

}