package ru.jamsys.core.extension.line.writer;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class LineWriterList implements LineWriter {

    List<String> result = new ArrayList<>();

    @Override
    public String getIndent() {
        return "   ";
    }

    @Override
    public void addLine(String s) {
        result.add(s);
    }

}
