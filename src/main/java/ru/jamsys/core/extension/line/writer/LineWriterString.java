package ru.jamsys.core.extension.line.writer;

import java.io.StringWriter;
import java.util.List;

public class LineWriterString implements LineWriter {

    private final StringWriter sw = new StringWriter();

    @Override
    public String getIndent() {
        return "\t";
    }

    @Override
    public void addLine(String s) {
        sw.append(s).append("\r\n");
    }

    @Override
    public void addLineAll(List<String> list) {
        list.forEach(this::addLine);
    }

    @Override
    public String toString() {
        return sw.toString();
    }

}
