package ru.jamsys.core.i360.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@ToString(includeFieldNames = false, doNotUseGetters = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EntityChain {

    private final List<Entity> chain = new ArrayList<>();

    public EntityChain() {
    }

    public EntityChain add(Entity entity) {
        chain.add(entity);
        return this;
    }

    public EntityChain(Entity entity) {
        chain.add(entity);
    }

    @SuppressWarnings("unused")
    @JsonValue
    public List<String> toValue() {
        List<String> result = new ArrayList<>();
        chain.forEach(entity -> result.add(entity.getUuid()));
        return result;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        EntityChain entityChain = (EntityChain) object;
        return Objects.equals(chain, entityChain.chain);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(chain);
    }

}
