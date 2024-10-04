package ru.jamsys.core.i360.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.jamsys.core.flat.util.UtilJson;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"uuid", "data"})
public class EntityImpl implements Entity {

    private String uuid;

    private String data;

    public Entity newInstance(String json) throws Throwable {
        return UtilJson.toObject(json, EntityImpl.class);
    }

}
