package ru.jamsys.core.flat.template.twix;

import ru.jamsys.core.extension.EnumName;

public enum Dictionary implements EnumName {

    DOLLAR("$"),
    CURLY_BRACE_OPEN("{"),
    CURLY_BRACE_CLOSE("}"),
    ESCAPE("\\"),
    ANY("*");

    private final String alias;

    public String getAlias() {
        return alias;
    }

    Dictionary(String alias) {
        this.alias = alias;
    }

    public static Dictionary parse(String ch) {
        return switch (ch) {
            case "$" -> Dictionary.DOLLAR;
            case "{" -> Dictionary.CURLY_BRACE_OPEN;
            case "}" -> Dictionary.CURLY_BRACE_CLOSE;
            case "\\" -> Dictionary.ESCAPE;
            default -> Dictionary.ANY;
        };
    }
}
