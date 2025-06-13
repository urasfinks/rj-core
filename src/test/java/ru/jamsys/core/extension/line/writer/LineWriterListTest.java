package ru.jamsys.core.extension.line.writer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LineWriterListTest {

    private LineWriterList writer;

    @BeforeEach
    void setUp() {
        writer = new LineWriterList();
    }

    @Test
    void testAddLine() {
        writer.addLine("First line");
        assertEquals(List.of("First line"), writer.getResult());
    }

    @Test
    void testAddMultipleLines() {
        writer.addLine("Line1");
        writer.addLine("Line2");
        assertEquals(List.of("Line1", "Line2"), writer.getResult());
    }

    @Test
    void testAddLineAll() {
        List<String> lines = List.of("One", "Two", "Three");
        writer.addLineAll(lines);
        assertEquals(lines, writer.getResult());
    }

    @Test
    void testAddLineIndent() {
        writer.addLineIndent("Indented");
        assertEquals(List.of("   Indented"), writer.getResult());
    }

    @Test
    void testGetIndent() {
        assertEquals("   ", writer.getIndent());
    }

    @Test
    void testEmptyInitially() {
        assertTrue(writer.getResult().isEmpty());
    }

}