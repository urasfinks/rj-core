package ru.jamsys.core.i360.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.i360.scale.ScaleType;
import ru.jamsys.core.i360.scope.Scope;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EntityImpl implements Entity {

    final private String uuid;

    private String data;

    @JsonIgnore
    @ToString.Exclude
    private Scope scope;

    @SuppressWarnings("unused") // Через рефлексию вызывается
    public EntityImpl(Map<String, Object> map, Scope scope) {
        this.uuid = map.containsKey("uuid") ? (String) map.get("uuid") : java.util.UUID.randomUUID().toString();
        this.data = (String) map.get("data");
        this.scope = scope;
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

    @JsonIgnore
    @Override
    public void getVariant(ScaleType type, Set<EntityChain> result) {
        result.add(new EntityChain(this));
        switch (type) {
            case EQUALS -> {
                scope.getRepositoryScale().getByLeft(new EntityChain(this), ScaleType.EQUALS).forEach(scale -> {
                    result.add(scale.getRight());
                });
            }
            case GENERALIZATION -> {
                scope.getRepositoryScale().getByLeft(new EntityChain(this), ScaleType.GENERALIZATION).forEach(scale -> {
                    scope.getRepositoryScale().getByRight(scale.getRight(), ScaleType.GENERALIZATION).forEach(scale1 -> {
                        result.add(scale1.getLeft());
                    });
                });
            }
            case CONSEQUENCE -> {
                scope.getRepositoryScale().getByLeft(new EntityChain(this), ScaleType.CONSEQUENCE).forEach(scale -> {
                    result.add(scale.getRight());
                });
            }
        }
    }

}
