package ru.jamsys.core.i360.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.jamsys.core.flat.util.UtilJson;

import java.util.Objects;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"uuid", "data"})
public class EntityImpl implements Entity {

    private String uuid;

    private String data;

    @SuppressWarnings("unused")
    public EntityImpl() {
    }

    public EntityImpl(String uuid, String data) {
        this.uuid = uuid;
        this.data = data;
    }

    public Entity newInstance(String json) throws Throwable {
        return UtilJson.toObject(json, EntityImpl.class);
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

}
