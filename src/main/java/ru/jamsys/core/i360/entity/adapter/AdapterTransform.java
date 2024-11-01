package ru.jamsys.core.i360.entity.adapter;

import lombok.Getter;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.entity.adapter.relation.RelationsType;
import ru.jamsys.core.i360.entity.adapter.transform.TransformType;
import ru.jamsys.core.i360.scope.Scope;

import java.util.List;
import java.util.Map;

// Трансформация цепочки
// Аргументы - одна цепочка

@Getter
public class AdapterTransform extends AbstractAdapter {

    private final TransformType type;

    private final String cls;

    public AdapterTransform(Map<String, Object> map, Scope scope) {
        super(map, scope);

        this.cls = (String) map.get("class");
        this.type = TransformType.valueOfCamelCase((String) map.get("type"));
    }

    @Override
    public EntityChain transform(EntityChain entityChain) {
        return type.transform(entityChain);
    }

    @Override
    public Map<String, Object> toValue() {
        return new HashMapBuilder<String, Object>()
                .append("class", cls)
                .append("type", type)
                ;
    }

}
