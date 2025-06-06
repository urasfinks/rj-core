package ru.jamsys.core.flat.util;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import ru.jamsys.core.App;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UtilJson {

    public static final ObjectMapper objectMapper = new ObjectMapper();
    public static final ObjectMapper objectMapperSkipUnknown = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    public static final ObjectMapper objectMapperPretty = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public static ReadContext getContext(String json) {
        return JsonPath.parse(json);
    }

    //https://github.com/json-path/JsonPath
    public static void selector(String json, Map<String, String> selector, Map<String, Object> res) {
        ReadContext ctx = getContext(json);
        selector.forEach((s, s2) -> res.put(s, ctx.read(s2)));
    }

    public static Object selector(String json, String selector) {
        ReadContext ctx = getContext(json);
        return ctx.read(selector);
    }

    public static void selector(String json, Map<String, String> selector, Map<String, Object> res, String def) {
        ReadContext ctx = getContext(json);
        selector.forEach((s, s2) -> {
            try {
                res.put(s, ctx.read(s2));
            } catch (Throwable th) {
                res.put(s, def);
            }
        });
    }

    public static String toString(Object object, String def) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            App.error(e);
        }
        return def;
    }

    public static String toString(Object object) throws Throwable {
        return objectMapper.writeValueAsString(object);
    }

    public static String toStringPretty(Object object, String def) {
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
        try {
            return objectMapperPretty.writer(prettyPrinter).writeValueAsString(object);
        } catch (Exception e) {
            App.error(e);
        }
        return def;
    }

    public static <T> T toObject(String json, Class<T> cls) throws Throwable {
        return objectMapperSkipUnknown.readValue(json, cls);
    }

    public static Map<String, Object> getMapOrThrow(String json) throws Throwable {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) objectMapper.readValue(json, Map.class);
            return map;
        } catch (Throwable th) {
            UtilLog.printError(json);
            throw th;
        }
    }

    public static List<Object> getListOrThrow(String json) throws Throwable {
        @SuppressWarnings("unchecked")
        List<Object> list = objectMapper.readValue(json, List.class);
        return list;
    }

    // Возникла потребность хранить состояние объекта на момент времени.
    // То есть в trace вставляется объект, Json сериализация выполняется в конце, а объект могли в разных местах поменять.
    // Получается на всех инстанция в отладке видим один и тот-же объект, что приводит к сложности анализа
    public static Object toLog(Object object) {
        if (object == null) {
            return null;
        }
        Map<String, Object> result = new HashMap<>();
        if (object instanceof String && object.toString().isEmpty()) {
            return "";
        }
        String json = toString(object, "{}");
        // Не смогли ничего путного сделать
        if (json == null) {
            return object;
        }
        // Бывает такое, что объект был строкой, что вернёт двойные апострофы, а нам такое не надо
        if (json.startsWith("\"")) {
            return json.substring(1, json.length() - 1);
        }
        if (json.startsWith("[")) {
            try {
                return getListOrThrow(json);
            } catch (Throwable th) {
                result.put("{error parsing list}", th.getMessage());
                result.put("origin_object", object);
                result.put("origin_object_serialize", json);
            }
        } else {
            try {
                result.putAll(getMapOrThrow(json));
            } catch (Throwable th) {
                result.put("{error parsing map}", th.getMessage());
                result.put("origin_object", object);
                result.put("origin_object_serialize", json);
            }
        }
        return result;
    }

    public static <T> T mapToObject(Map<String, Object> map, Class<T> cls) {
        return objectMapper.convertValue(map, cls);
    }

}
