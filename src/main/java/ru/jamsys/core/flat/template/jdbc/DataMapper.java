package ru.jamsys.core.flat.template.jdbc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ru.jamsys.core.flat.UtilCodeStyle;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilText;

import java.util.LinkedHashMap;
import java.util.Map;

public class DataMapper<T> {

    public enum TransformCodeStyle {
        CAMEL_TO_SNAKE,
        SNAKE_TO_CAMEL,
        NONE
    }

    @JsonIgnore
    public T fromMap(Map<String, Object> map, TransformCodeStyle transformCodeStyle) {
        @SuppressWarnings("unchecked")
        T t = (T) UtilJson.mapToObject(transform(map, transformCodeStyle), this.getClass());
        return t;
    }

    @JsonIgnore
    public T fromJson(String json, TransformCodeStyle transformCodeStyle) throws Throwable {
        return fromMap(UtilJson.getMapOrThrow(json), transformCodeStyle);
    }

    @JsonIgnore
    public Map<String, Object> toMap(TransformCodeStyle transformCodeStyle) {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = UtilJson.objectMapper.convertValue(this, LinkedHashMap.class);
        return transform(map, transformCodeStyle);
    }

    @JsonIgnore
    public String toJson(boolean pretty, TransformCodeStyle transformCodeStyle) {
        @SuppressWarnings("unchecked")
        Map<String, Object> map = UtilJson.objectMapper.convertValue(this, LinkedHashMap.class);
        Map<String, Object> result = transform(map, transformCodeStyle);
        return pretty ? UtilJson.toStringPretty(result, "{}") : UtilJson.toString(result, "{}");
    }

    private Map<String, Object> transform(Map<String, Object> map, TransformCodeStyle transformCodeStyle) {
        if (transformCodeStyle.equals(TransformCodeStyle.NONE)) {
            return map;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        switch (transformCodeStyle) {
            case SNAKE_TO_CAMEL ->
                    map.forEach((s, o) -> result.put(UtilText.firstCharToLowerCase(UtilCodeStyle.snakeToCamel(s)), o));
            case CAMEL_TO_SNAKE -> map.forEach((s, o) -> result.put(UtilCodeStyle.camelToSnake(s).toLowerCase(), o));
        }
        return result;
    }

}
