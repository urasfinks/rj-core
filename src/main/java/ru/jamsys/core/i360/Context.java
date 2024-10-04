package ru.jamsys.core.i360;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.ToString;
import ru.jamsys.core.i360.entity.Entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Context {

    private final List<Entity> listEntity = new ArrayList<>();

    @SuppressWarnings("unused")
    @JsonValue
    public List<String> toValue() {
        List<String> result = new ArrayList<>();
        listEntity.forEach(entity -> result.add(entity.getUuid()));
        return result;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Context context = (Context) object;
        return Objects.equals(listEntity, context.listEntity);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(listEntity);
    }

}
