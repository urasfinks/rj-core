package ru.jamsys.core.extension.line.writer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LineWriterStringTest {

    private LineWriterString writer;

    @BeforeEach
    void setUp() {
        writer = new LineWriterString();
    }

    @Test
    void testAddLine() {
        writer.addLine("Hello");
        assertEquals("Hello\r\n", writer.toString());
    }

    @Test
    void testAddMultipleLines() {
        writer.addLine("Line1");
        writer.addLine("Line2");
        assertEquals("Line1\r\nLine2\r\n", writer.toString());
    }

    @Test
    void testAddLineAll() {
        writer.addLineAll(List.of("A", "B", "C"));
        assertEquals("A\r\nB\r\nC\r\n", writer.toString());
    }

    @Test
    void testGetIndent() {
        assertEquals("\t", writer.getIndent());
    }

    @Test
    void testAddLineIndent() {
        writer.addLineIndent("Indented line");
        assertEquals("\tIndented line\r\n", writer.toString());
    }

    @Test
    void testEmptyWriter() {
        assertEquals("", writer.toString());
    }
}