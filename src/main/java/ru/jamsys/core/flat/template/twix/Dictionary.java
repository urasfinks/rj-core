package ru.jamsys.core.flat.template.twix;

import lombok.Getter;
import ru.jamsys.core.extension.CamelNormalization;

@Getter
public enum Dictionary implements CamelNormalization {

    DOLLAR("$"),
    CURLY_BRACE_OPEN("{"),
    CURLY_BRACE_CLOSE("}"),
    ESCAPE("\\"),
    ANY("*");

    private final String alias;

    Dictionary(String alias) {
        this.alias = alias;
    }

    public static Dictionary parse(String ch) {
        if (ch == null) {
            throw new IllegalArgumentException("Input character cannot be null");
        }
        return switch (ch) {
            case "$" -> Dictionary.DOLLAR;
            case "{" -> Dictionary.CURLY_BRACE_OPEN;
            case "}" -> Dictionary.CURLY_BRACE_CLOSE;
            case "\\" -> Dictionary.ESCAPE;
            default -> Dictionary.ANY;
        };
    }

}
