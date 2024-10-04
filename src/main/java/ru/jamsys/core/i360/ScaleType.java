package ru.jamsys.core.i360;

public enum ScaleType {

    EQUALS("="), // Эквиваленция
    FOLLOW("=>"), // Следствие
    GENERALIZATION("~>"), // Обобщение

    NOT_EQUALS("!="),
    NOT_FOLLOW("!=>"),
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
