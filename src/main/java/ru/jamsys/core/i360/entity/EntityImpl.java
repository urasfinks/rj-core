package ru.jamsys.core.i360.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.jamsys.core.extension.builder.HashMapBuilder;

import java.util.Map;
import java.util.Objects;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EntityImpl implements Entity {

    final private String uuid;

    private String data;

    @SuppressWarnings("unused") // Через рефлексию вызывается
    public EntityImpl(Map<String, Object> map) {
        this.uuid = map.containsKey("uuid") ? (String) map.get("uuid") : java.util.UUID.randomUUID().toString();
        this.data = (String) map.get("data");
    }

    public EntityImpl(String uuid, String data) {
        this.uuid = uuid;
        this.data = data;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        EntityImpl entity = (EntityImpl) object;
        return Objects.equals(uuid, entity.uuid) && Objects.equals(data, entity.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, data);
    }

    @SuppressWarnings("unused")
    @JsonValue
    public Map<String, String> toValue() {
        return new HashMapBuilder<String, String>().append("data", data);
    }

}
