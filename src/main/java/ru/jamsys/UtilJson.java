package ru.jamsys;

import java.util.Map;

public class UtilJson {
    public static Object selector(Map<String, Object> obj, String selector) {
        String[] split = selector.split("\\.");
        Map<String, Object> target = obj;
        for (int i = 0; i < split.length; i++) {
            if (!target.containsKey(split[i])) {
                return null;
            }
            if (i == split.length - 1) {
                return target.get(split[i]);
            } else {
                target = (Map<String, Object>) target.get(split[i]);
            }
        }
        return null;
    }
}
