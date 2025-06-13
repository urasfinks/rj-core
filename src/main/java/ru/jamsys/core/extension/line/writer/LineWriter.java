package ru.jamsys.core.extension.line.writer;

import java.util.List;

public interface LineWriter {

    String getIndent();

    void addLine(String s);

    void addLineAll(List<String> list);

    default void addLineIndent(String s) {
        addLine(getIndent() + s);
    }

}
