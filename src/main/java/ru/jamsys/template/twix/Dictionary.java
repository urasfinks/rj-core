package ru.jamsys.template.twix;

public enum Dictionary {
    DOLLAR("$"),
    CURLY_BRACE_OPEN("{"),
    CURLY_BRACE_CLOSE("}"),
    ESCAPE("\\"),
    ANY("*");

    private final String name;

    public String getName() {
        return name;
    }

    Dictionary(String name) {
        this.name = name;
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
