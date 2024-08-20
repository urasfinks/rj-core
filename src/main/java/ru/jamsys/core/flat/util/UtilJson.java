package ru.jamsys.core.flat.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import org.springframework.lang.Nullable;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.exception.ForwardException;

import java.util.List;
import java.util.Map;

public class UtilJson {

    static ObjectMapper objectMapper = new ObjectMapper();
    static ObjectMapper objectMapperPretty;
    static ObjectMapper objectMapper2 = new ObjectMapper();

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

    public static ReadContext getContext(String json){
        return JsonPath.parse(json);
    }

    //https://github.com/json-path/JsonPath
    public static void selector(String json, Map<String, String> selector, Map<String, Object> res) {
        ReadContext ctx = getContext(json);
        selector.forEach((s, s2) -> res.put(s, ctx.read(s2)));
    }

    @SuppressWarnings("unused")
    @Nullable
    public static String toString(Object object, String def) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            App.error(e);
        }
        return def;
    }

    @SuppressWarnings("unused")
    @Nullable
    public static String toStringPretty(Object object, String def) {
        DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
        prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
        if (objectMapperPretty == null) {
            objectMapperPretty = new ObjectMapper();
            objectMapperPretty.enable(SerializationFeature.INDENT_OUTPUT);
        }
        try {
            return objectMapper.writer(prettyPrinter).writeValueAsString(object);
        } catch (Exception e) {
            App.error(e);
        }
        return def;
    }

    @SuppressWarnings("unused")
    public static <T> JsonEnvelope<T> toObjectOverflow(String json, Class<T> t) {
        JsonEnvelope<T> ret = new JsonEnvelope<>();
        try {
            objectMapper2.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            ret.setObject(objectMapper2.readValue(json, t));
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
            ret.setObject(objectMapper.readValue(json, new TypeReference<>() {
            }));
        } catch (Exception e) {
            ret.setException(e);
            App.error(e);
        }
        return ret;
    }

    @SuppressWarnings("unused")
    public static <V> JsonEnvelope<List<V>> toList(String json) {
        JsonEnvelope<List<V>> ret = new JsonEnvelope<>();
        try {
            ret.setObject(objectMapper.readValue(json, new TypeReference<>() {
            }));
        } catch (Exception e) {
            ret.setException(e);
            App.error(e);
        }
        return ret;
    }

}
