package ru.jamsys.core.i360.scale;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ScaleTypeRelation {

    EQUALS("="), // Эквиваленция
    CONSEQUENCE("=>"), // Следствие / Умозаключение
    GENERALIZATION("~>"), // Обобщение
    PROPERTY("."), // Свойство

    NOT_EQUALS("!="),
    NOT_CONSEQUENCE("!=>"),
    NOT_GENERALIZATION("!~>"),
    NOT_PROPERTY("!.");

    final String reduction;

    ScaleTypeRelation(String reduction) {
        this.reduction = reduction;
    }

    public static ScaleTypeRelation valueOfReduction(String reduction) {
        for (ScaleTypeRelation scaleTypeRelation : ScaleTypeRelation.values()) {
            if (scaleTypeRelation.reduction.equals(reduction)) {
                return scaleTypeRelation;
            }
        }
        throw new RuntimeException("ScaleType.valueOf(" + reduction + ") not found");
    }

    @SuppressWarnings("unused")
    @JsonValue
    public String toValue() {
        return reduction;
    }

}
