package ru.jamsys.core.i360.entity.adapter;

import lombok.Getter;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.i360.Context;
import ru.jamsys.core.i360.scope.Scope;

import java.util.List;
import java.util.Map;

@Getter
public class Sequence extends AbstractAdapter {

    private final Context entity;

    private Integer min;

    private Integer max;

    private final String cls;

    public Sequence(Map<String, Object> map, Scope scope) {
        super(map, scope);

        @SuppressWarnings("unchecked")
        List<String> listEntity = (List<String>) map.get("entity");
        entity = scope.getContext(listEntity);

        if (map.containsKey("min") && map.get("min") != null) {
            min = Integer.parseInt(map.get("min") + "");
        }
        if (map.containsKey("max") && map.get("max") != null) {
            max = Integer.parseInt(map.get("max") + "");
        }
        this.cls = (String) map.get("class");
    }

    @Override
    public Context transform(Context context) {
        return null;
    }

    @Override
    public Map<String, Object> toValue() {
        return new HashMapBuilder<String, Object>()
                .append("class", cls)
                .append("entity", entity)
                .append("min", max)
                .append("max", max)
                ;
    }

}
