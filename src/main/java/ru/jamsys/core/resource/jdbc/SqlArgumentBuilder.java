package ru.jamsys.core.resource.jdbc;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Setter
@Getter
@Accessors(chain = true)
public class SqlArgumentBuilder {

    boolean batchMaybeEmpty = true;

    final List<Map<String, Object>> listArgument = new ArrayList<>();

    public SqlArgumentBuilder() {
        listArgument.add(new LinkedHashMap<>());
    }

    public List<Map<String, Object>> get() {
        // Просто отрезаем пустой элемент, если он был сформирован nextBatch() заканчивающим пачку
        if (listArgument.size() > 1 && listArgument.getLast().isEmpty()) {
            listArgument.removeLast();
        }
        if (listArgument.size() == 1 && listArgument.getFirst().isEmpty() && !isBatchMaybeEmpty()) {
            return null;
        }
        return listArgument;
    }

    public SqlArgumentBuilder add(String key, Object obj) {
        listArgument.getLast().put(key, obj);
        return this;
    }

    public SqlArgumentBuilder add(Map<String, ?> map) {
        listArgument.getLast().putAll(map);
        return this;
    }

    @SuppressWarnings("all")
    public SqlArgumentBuilder nextBatch() {
        if (!isBatchMaybeEmpty() && listArgument.getLast().isEmpty()) {
            return this;
        }
        listArgument.add(new LinkedHashMap<>());
        return this;
    }

}
