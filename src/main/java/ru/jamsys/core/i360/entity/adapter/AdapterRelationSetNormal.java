package ru.jamsys.core.i360.entity.adapter;

import lombok.Getter;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.entity.adapter.relation.normal.RelationType;
import ru.jamsys.core.i360.scope.Scope;

import java.util.List;
import java.util.Map;

// Отношение множеств - Диаграммы Эйлера
// Аргументы - две цепочки сущностей

@Getter
public class AdapterRelationSetNormal extends AbstractAdapter {

    private final EntityChain entityChain;

    private final RelationType type;

    private final String cls;

    public AdapterRelationSetNormal(Map<String, Object> map, Scope scope) {
        super(map, scope);

        if (!map.containsKey("entityChain")) {
            throw new RuntimeException("Undefined entity context");
        }
        @SuppressWarnings("unchecked")
        List<String> listEntity = (List<String>) map.get("entityChain");
        entityChain = scope.getRepositoryEntityChain().getByUuids(listEntity);

        this.cls = (String) map.get("class");
        this.type = RelationType.valueOfCamelCase((String) map.get("type"));
    }

    @Override
    public EntityChain transform(EntityChain entityChain) {
        return type.getRelation().compute(entityChain, this.entityChain);
    }

    @Override
    public Map<String, Object> toValue() {
        return new HashMapBuilder<String, Object>()
                .append("class", cls)
                .append("entityChain", entityChain)
                .append("type", type)
                ;
    }

}
