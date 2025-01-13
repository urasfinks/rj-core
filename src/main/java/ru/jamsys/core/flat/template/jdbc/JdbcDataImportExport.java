package ru.jamsys.core.flat.template.jdbc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ru.jamsys.core.flat.util.Util;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class JdbcDataImportExport<T> {

    @JsonIgnore
    public T fromMap(Map<String, Object> map) {
        Map<String, Object> prepare = new HashMap<>();
        map.forEach((s, o) -> prepare.put(Util.firstCharToLowerCase(Util.snakeToCamel(s)), o));
        @SuppressWarnings("unchecked")
        T t = (T) Util.mapToObject(prepare, this.getClass());
        return t;
    }

    @JsonIgnore
    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        @SuppressWarnings("unchecked")
        Map<String, Object> map = Util.objectMapper.convertValue(this, LinkedHashMap.class);
        map.forEach((s, o) -> result.put(Util.camelToSnake(s).toLowerCase(), o));
        return result;
    }

}
