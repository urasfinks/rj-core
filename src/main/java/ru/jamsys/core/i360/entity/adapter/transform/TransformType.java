package ru.jamsys.core.i360.entity.adapter.transform;

import com.fasterxml.jackson.annotation.JsonValue;
import ru.jamsys.core.extension.CamelNormalization;
import ru.jamsys.core.i360.entity.EntityChain;

public enum TransformType implements CamelNormalization {

    REVERSE(new Reverse()); // Задом на перёд, для решения задач удаления с конца всех нулей например

    final private Transform transform;

    TransformType(Transform transform) {
        this.transform = transform;
    }

    public EntityChain transform(EntityChain entityChain) {
        return transform.transform(entityChain);
    }

    public static TransformType valueOfCamelCase(String type) {
        TransformType[] values = values();
        for (TransformType relationsType : values) {
            if (relationsType.getNameCamel().equals(type)) {
                return relationsType;
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    @JsonValue
    public String toValue() {
        return getNameCamel();
    }

}
