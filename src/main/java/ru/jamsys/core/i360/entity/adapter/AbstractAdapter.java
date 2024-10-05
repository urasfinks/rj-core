package ru.jamsys.core.i360.entity.adapter;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.i360.entity.Adapter;
import ru.jamsys.core.i360.scope.Scope;

import java.util.Map;

@Getter
public abstract class AbstractAdapter implements Adapter {

    private final String uuid;

    public AbstractAdapter(Map<String, Object> map, Scope scope) {
        this.uuid = map.containsKey("uuid") ? (String) map.get("uuid") : java.util.UUID.randomUUID().toString();
    }

    @SuppressWarnings("unused")
    @JsonValue
    public Map<String, Object> toValue() {
        return new HashMapBuilder<>();
    }

}
