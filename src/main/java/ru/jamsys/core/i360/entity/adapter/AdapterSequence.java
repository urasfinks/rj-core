package ru.jamsys.core.i360.entity.adapter;

import lombok.Getter;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.scope.Scope;

import java.util.List;
import java.util.Map;

@Getter
public class AdapterSequence extends AbstractAdapter {

    private final EntityChain entityChain;

    private Integer min;

    private Integer max;

    private final String cls;

    public AdapterSequence(Map<String, Object> map, Scope scope) {
        super(map, scope);

        if (!map.containsKey("entityChain")) {
            throw new RuntimeException("Undefined entity context");
        }

        @SuppressWarnings("unchecked")
        List<String> listEntity = (List<String>) map.get("entityChain");
        entityChain = scope.getRepositoryEntityChain().getByUuids(listEntity);

        if (map.containsKey("min") && map.get("min") != null) {
            min = Integer.parseInt(map.get("min") + "");
        }
        if (map.containsKey("max") && map.get("max") != null) {
            max = Integer.parseInt(map.get("max") + "");
        }
        this.cls = (String) map.get("class");
    }

    @Override
    public EntityChain transform(EntityChain entityChain) {
        return null;
    }

    @Override
    public Map<String, Object> toValue() {
        return new HashMapBuilder<String, Object>()
                .append("class", cls)
                .append("entityChain", entityChain)
                .append("min", max)
                .append("max", max)
                ;
    }

}
