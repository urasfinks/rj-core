package ru.jamsys.core.flat.template.jdbc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilJson;

import java.util.LinkedHashMap;
import java.util.Map;

public class DataMapper<T> {

    public enum Transform {
        CAMEL_TO_SNAKE,
        SNAKE_TO_CAMEL,
        NONE
    }

    @JsonIgnore
    public T fromMap(Map<String, Object> map, Transform transform) {
        @SuppressWarnings("unchecked")
        T t = (T) Util.mapToObject(transform(map, transform), this.getClass());
        return t;
    }

    @JsonIgnore
    public T fromJson(String json, Transform transform) throws Throwable {
        return fromMap(UtilJson.getMapOrThrow(json), transform);
    }

    @JsonIgnore
    public Map<String, Object> toMap(Transform transform) {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = Util.objectMapper.convertValue(this, LinkedHashMap.class);
        return transform(map, transform);
    }

    @JsonIgnore
    public String toJson(boolean pretty, Transform transform) {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = Util.objectMapper.convertValue(this, LinkedHashMap.class);
        Map<String, Object> result = transform(map, transform);
        return pretty ? UtilJson.toStringPretty(result, "{}") : UtilJson.toString(result, "{}");
    }

    private Map<String, Object> transform(Map<String, Object> map, Transform transform) {
        if (transform.equals(Transform.NONE)) {
            return map;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        switch (transform) {
            case SNAKE_TO_CAMEL ->
                    map.forEach((s, o) -> result.put(Util.firstCharToLowerCase(Util.snakeToCamel(s)), o));
            case CAMEL_TO_SNAKE -> map.forEach((s, o) -> result.put(Util.camelToSnake(s).toLowerCase(), o));
        }
        return result;
    }

}
