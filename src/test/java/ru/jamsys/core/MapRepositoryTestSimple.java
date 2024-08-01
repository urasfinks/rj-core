package ru.jamsys.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.extension.RepositoryMap;

import java.util.HashMap;
import java.util.Map;

// IO time: 11ms
// COMPUTE time: 12ms

class TestRepositoryMapSimple {

    public static class P implements RepositoryMap<String, Object> {

        Map<String, Object> map = new HashMap<>();

        @Override
        public Map<String, Object> getRepositoryMap() {
            return map;
        }
    }

    @Test
    void test() {
        P p = new P();

        Assertions.assertEquals("y", p.setMapRepository("x", "y"));
        Assertions.assertEquals("y", p.setMapRepository("x", "y2"));
        Assertions.assertEquals("y", p.getRepositoryMap("x", String.class, ""));
        Assertions.assertEquals("def", p.getRepositoryMap("x1", String.class, "def"));

    }

}