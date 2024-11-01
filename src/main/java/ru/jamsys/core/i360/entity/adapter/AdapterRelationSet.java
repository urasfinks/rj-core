package ru.jamsys.core.i360.entity.adapter;

import lombok.Getter;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.entity.adapter.relation.RelationType;
import ru.jamsys.core.i360.scope.Scope;

import java.util.List;
import java.util.Map;

// Отношение множеств - Диаграммы Эйлера
// Аргументы - две последовательности сущностей

@Getter
public class AdapterRelationSet extends AbstractAdapter {

    private final EntityChain entityChain;

    private final RelationType type;

    private final String cls;

    public AdapterRelationSet(Map<String, Object> map, Scope scope) {
        super(map, scope);

        if (map.containsKey("entity")) {
            @SuppressWarnings("unchecked")
            List<String> listEntity = (List<String>) map.get("entity");
            entityChain = scope.getRepositoryEntityChain().getByUuids(listEntity);
        } else if (map.containsKey("entityChain")) {
            entityChain = (EntityChain) map.get("entityChain");
        } else {
            throw new RuntimeException("Undefined entity context");
        }

        this.cls = (String) map.get("class");
        this.type = RelationType.valueOfCamelCase((String) map.get("type"));
    }

    @Override
    public EntityChain transform(EntityChain entityChain) {
        return type.getRelation().relation(entityChain, this.entityChain);
    }

    @Override
    public Map<String, Object> toValue() {
        return new HashMapBuilder<String, Object>()
                .append("class", cls)
                .append("entity", entityChain)
                .append("type", type)
                ;
    }

}
