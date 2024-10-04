package ru.jamsys.core.i360.scale;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ScaleType {

    @JsonProperty("=")
    EQUALS("="), // Эквиваленция
    @JsonProperty("=>")
    FOLLOW("=>"), // Следствие
    @JsonProperty("~>")
    GENERALIZATION("~>"), // Обобщение

    @JsonProperty("!=")
    NOT_EQUALS("!="),
    @JsonProperty("!=>")
    NOT_FOLLOW("!=>"),
    @JsonProperty("!~>")
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

}
