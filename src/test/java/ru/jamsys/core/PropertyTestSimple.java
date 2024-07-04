package ru.jamsys.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.extension.property.Property;

import java.util.HashMap;
import java.util.Map;

// IO time: 11ms
// COMPUTE time: 12ms

class PropertyTestSimple {

    public static class P implements Property<String, Object> {

        Map<String, Object> map = new HashMap<>();

        @Override
        public Map<String, Object> getMapProperty() {
            return map;
        }
    }

    @Test
    void test() {
        P p = new P();

        Assertions.assertEquals("y", p.setProperty("x", "y"));
        Assertions.assertEquals("y", p.setProperty("x", "y2"));
        Assertions.assertEquals("y", p.getProperty("x", String.class, ""));
        Assertions.assertEquals("def", p.getProperty("x1", String.class, "def"));

    }

}