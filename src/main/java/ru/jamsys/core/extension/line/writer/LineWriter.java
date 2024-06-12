package ru.jamsys.core.extension.line.writer;

public interface LineWriter {

    String getIndent();

    void addLine(String s);

    default void addLineIndent(String s) {
        addLine(getIndent() + s);
    }

}
