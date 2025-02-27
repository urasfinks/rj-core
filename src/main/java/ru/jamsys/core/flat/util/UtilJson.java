package ru.jamsys.core.flat.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import org.springframework.lang.Nullable;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.exception.ForwardException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UtilJson {

    public static Object selector(Map<String, Object> obj, String selector) {
        String[] split = selector.split("\\.");
        Map<String, Object> target = obj;
        for (int i = 0; i < split.length; i++) {
            // Должна быть не конкурентная проверка
            if (!target.containsKey(split[i])) {
                return null;
            }
            if (i == split.length - 1) {
                return target.get(split[i]);
            } else {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> x = (Map<String, Object>) target.get(split[i]);
                    target = x;
                } catch (Throwable th) {
                    throw new ForwardException("selector: " + selector + "; ex: " + split[i], th);
                }
            }
        }
        return null;
    }

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

    @SuppressWarnings("unused")
    @Nullable
    public static String toString(Object object, String def) {
        try {
            return Util.objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            App.error(e);
        }
        return def;
    }

    public static String toString(Object object) throws Throwable {
        return Util.objectMapper.writeValueAsString(object);
    }

    @SuppressWarnings("unused")
    @Nullable
    public static String toStringPretty(Object object, String def) {
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
        try {
            return Util.objectMapperPretty.writer(prettyPrinter).writeValueAsString(object);
        } catch (Exception e) {
            App.error(e);
        }
        return def;
    }

    public static <T> T toObject(String json, Class<T> cls) throws Throwable {
        return Util.objectMapperSkipUnknown.readValue(json, cls);
    }

    @SuppressWarnings("unused")
    public static <T> JsonEnvelope<T> toObjectOverflow(String json, Class<T> t) {
        JsonEnvelope<T> ret = new JsonEnvelope<>();
        try {
            ret.setObject(Util.objectMapperSkipUnknown.readValue(json, t));
        } catch (Exception e) {
            ret.setException(e);
            App.error(e);
        }
        return ret;
    }

    /* Example:
    *   WrapJsonToObject<Map<String, Map<String, Object>>> mapWrapJsonToObject = UtilJson.toMap(message.getBody());
        Map<String, Object> parsedJson = mapWrapJsonToObject.getObject().get("request");
    * */
    @SuppressWarnings("unused")
    public static <K, V> JsonEnvelope<Map<K, V>> toMap(String json) {
        JsonEnvelope<Map<K, V>> ret = new JsonEnvelope<>();
        try {
            ret.setObject(Util.objectMapper.readValue(json, new TypeReference<>() {
            }));
        } catch (Exception e) {
            ret.setException(e);
            App.error(e);
        }
        return ret;
    }

    public static Map<String, Object> getMapOrThrow(String json) throws Throwable {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) Util.objectMapper.readValue(json, Map.class);
            return map;
        } catch (Throwable th) {
            Util.logConsole(UtilJson.class, json);
            throw th;
        }
    }

    public static List<Object> getListOrThrow(String json) throws Throwable {
        @SuppressWarnings("unchecked")
        List<Object> list = Util.objectMapper.readValue(json, List.class);
        return list;
    }

    @SuppressWarnings("unused")
    public static <V> JsonEnvelope<List<V>> toList(String json) {
        JsonEnvelope<List<V>> ret = new JsonEnvelope<>();
        try {
            ret.setObject(Util.objectMapper.readValue(json, new TypeReference<>() {
            }));
        } catch (Exception e) {
            ret.setException(e);
            App.error(e);
        }
        return ret;
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

}
