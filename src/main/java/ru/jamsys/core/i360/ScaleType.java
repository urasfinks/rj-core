package ru.jamsys.core.i360;

public enum ScaleType {

    EQUALS("="), // Согласие (да/равно)
    FOLLOW("=>"), // Следствие
    GENERALIZATION("~>"), // Обобщение

    NOT_EQUALS("!="),
    NOT_FOLLOW("!=>"),
    NOT_GENERALIZATION("!~>");

    final String reduction;

    ScaleType(String reduction) {
        this.reduction = reduction;
    }
}
