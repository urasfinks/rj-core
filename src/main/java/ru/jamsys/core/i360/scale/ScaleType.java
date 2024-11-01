package ru.jamsys.core.i360.scale;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ScaleType {

    EQUALS("="), // Эквиваленция
    CONSEQUENCE("=>"), // Следствие
    GENERALIZATION("~>"), // Обобщение

    NOT_EQUALS("!="),
    NOT_CONSEQUENCE("!=>"),
    NOT_GENERALIZATION("!~>");

    final String reduction;

    ScaleType(String reduction) {
        this.reduction = reduction;
    }

    public static ScaleType valueOfReduction(String reduction) {
        for (ScaleType scaleType : ScaleType.values()) {
            if (scaleType.reduction.equals(reduction)) {
                return scaleType;
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
