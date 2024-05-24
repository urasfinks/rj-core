package ru.jamsys.core.extension;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PropertyTest {

    public static class P implements Property<String>{

        Map<String, Object> map = new HashMap<>();

        @Override
        public Map<String, Object> getMapProperty() {
            return map;
        }
    }

    @Test
    void test(){
        P p = new P();
        String s = p.setProperty("x", "y");
        String x = p.getProperty("x", String.class, "");
    }

}