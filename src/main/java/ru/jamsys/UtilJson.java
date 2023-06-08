package ru.jamsys;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.lang.Nullable;

import java.util.Map;

public class UtilJson {

    static ObjectMapper objectMapper = new ObjectMapper();

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

    @SuppressWarnings("unused")
    @Nullable
    public static String toString(Object object, String def) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return def;
    }

    @SuppressWarnings("unused")
    @Nullable
    public static String toStringPretty(Object object, String def) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return def;
    }

    static ObjectMapper objectMapper2 = new ObjectMapper();

    @SuppressWarnings("unused")
    public static <T> WrapJsonToObject<T> toObjectOverflow(String json, Class<T> t) {
        WrapJsonToObject<T> ret = new WrapJsonToObject<>();
        try {
            objectMapper2.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            ret.setObject(objectMapper2.readValue(json, t));
        } catch (Exception e) {
            ret.setException(e);
            e.printStackTrace();
        }
        return ret;
    }

    @SuppressWarnings("unused")
    public static <T> WrapJsonToObject<T> toObject(String json, Class<T> t) {
        WrapJsonToObject<T> ret = new WrapJsonToObject<>();
        try {
            ret.setObject(objectMapper.readValue(json, t));
        } catch (Exception e) {
            ret.setException(e);
            e.printStackTrace();
        }
        return ret;
    }
}
