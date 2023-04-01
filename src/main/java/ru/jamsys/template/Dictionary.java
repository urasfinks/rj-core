package ru.jamsys.template;

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
        switch (ch) {
            case "$":
                return Dictionary.DOLLAR;
            case "{":
                return Dictionary.CURLY_BRACE_OPEN;
            case "}":
                return Dictionary.CURLY_BRACE_CLOSE;
            case "\\":
                return Dictionary.ESCAPE;
        }
        return Dictionary.ANY;
    }
}
