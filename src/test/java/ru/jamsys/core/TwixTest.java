package ru.jamsys.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.template.twix.TemplateItemTwix;
import ru.jamsys.core.flat.template.twix.TemplateTwix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// IO time: 13ms
// COMPUTE time: 13ms

class TwixTest {

    @Test
    void template() {
        Map<String, String> args = new HashMap<>();
        args.put("name", "Ura");
        Assertions.assertEquals("Hello Ura", TemplateTwix.template("Hello ${name}", args));
        Assertions.assertEquals("Hello ${name}", TemplateTwix.template("Hello \\${name}", args));
    }

    @Test
    void templateNull() {
        Map<String, String> args = new HashMap<>();
        Assertions.assertEquals("Hello ${name}", TemplateTwix.template("Hello ${name}", args, true));
    }

    @Test
    void testGetParsedTemplateWithVariable() {
        String template = "Hello ${user}!";
        List<TemplateItemTwix> items = TemplateTwix.getParsedTemplate(template);

        assertEquals(3, items.size());

        assertTrue(items.get(0).isStaticFragment());
        assertEquals("Hello ", items.get(0).getValue());

        assertFalse(items.get(1).isStaticFragment());
        assertEquals("user", items.get(1).getValue());

        assertTrue(items.get(2).isStaticFragment());
        assertEquals("!", items.get(2).getValue());
    }

    @Test
    void testTemplateReplacement() {
        String template = "Price: ${amount} USD";
        Map<String, String> args = Map.of("amount", "100");
        String result = TemplateTwix.template(template, args);

        assertEquals("Price: 100 USD", result);
    }

    @Test
    void testTemplateWithMissingVariableAndForwardEnabled() {
        String template = "Hello ${name}";
        Map<String, String> args = Map.of(); // empty
        String result = TemplateTwix.template(template, args, true);

        assertEquals("Hello ${name}", result);
    }

    @Test
    void testTemplateWithMissingVariableAndForwardDisabled() {
        String template = "Hello ${name}";
        Map<String, String> args = Map.of(); // empty
        String result = TemplateTwix.template(template, args, false);

        assertEquals("Hello null", result);
    }

    @Test
    void testOnlyTextTemplate() {
        String template = "No variables here.";
        List<TemplateItemTwix> items = TemplateTwix.getParsedTemplate(template);

        assertEquals(1, items.size());
        assertTrue(items.getFirst().isStaticFragment());
        assertEquals("No variables here.", items.getFirst().getValue());
    }

    @Test
    void testOnlyVariableTemplate() {
        String template = "${var}";
        List<TemplateItemTwix> items = TemplateTwix.getParsedTemplate(template);

        assertEquals(1, items.size());
        assertFalse(items.getFirst().isStaticFragment());
        assertEquals("var", items.getFirst().getValue());
    }

    @Test
    void testEmptyTemplate() {
        List<TemplateItemTwix> items = TemplateTwix.getParsedTemplate("");
        assertTrue(items.isEmpty());
    }

    @Test
    void testCase1() {
        List<TemplateItemTwix> actual = TemplateTwix.getParsedTemplate("H $ryy");

        assertEquals(1, actual.size(), "#1 - size");

        TemplateItemTwix item = actual.getFirst();
        assertTrue(item.isStaticFragment(), "#1 - fragment should be static");
        assertEquals("H $ryy", item.getValue(), "#1 - value mismatch");
    }

    @Test
    void testCase2() {
        List<TemplateItemTwix> actual = TemplateTwix.getParsedTemplate("H ${x}");
        assertEquals(2, actual.size(), "#2 - size");
        assertTrue(actual.get(0).isStaticFragment(), "#2 - part 0 static");
        assertEquals("H ", actual.get(0).getValue(), "#2 - part 0 value");
        assertFalse(actual.get(1).isStaticFragment(), "#2 - part 1 static");
        assertEquals("x", actual.get(1).getValue(), "#2 - part 1 value");
    }

    @Test
    void testCase3() {
        List<TemplateItemTwix> actual = TemplateTwix.getParsedTemplate("Hello ${world}");
        assertEquals(2, actual.size(), "#3 - size");
        assertTrue(actual.get(0).isStaticFragment(), "#3 - part 0 static");
        assertEquals("Hello ", actual.get(0).getValue(), "#3 - part 0 value");
        assertFalse(actual.get(1).isStaticFragment(), "#3 - part 1 static");
        assertEquals("world", actual.get(1).getValue(), "#3 - part 1 value");
    }

    @Test
    void testCase4() {
        List<TemplateItemTwix> actual = TemplateTwix.getParsedTemplate("H${x}");
        assertEquals(2, actual.size(), "#4 - size");
        assertTrue(actual.get(0).isStaticFragment(), "#4 - part 0 static");
        assertEquals("H", actual.get(0).getValue(), "#4 - part 0 value");
        assertFalse(actual.get(1).isStaticFragment(), "#4 - part 1 static");
        assertEquals("x", actual.get(1).getValue(), "#4 - part 1 value");
    }

    @Test
    void testCase5() {
        List<TemplateItemTwix> actual = TemplateTwix.getParsedTemplate("${x}");
        assertEquals(1, actual.size(), "#5 - size");
        assertFalse(actual.getFirst().isStaticFragment(), "#5 - part 0 static");
        assertEquals("x", actual.getFirst().getValue(), "#5 - part 0 value");
    }

    @Test
    void testCase6() {
        List<TemplateItemTwix> actual = TemplateTwix.getParsedTemplate("${hello} world");
        assertEquals(2, actual.size(), "#6 - size");

        assertFalse(actual.get(0).isStaticFragment(), "#6 - part 0 static");
        assertEquals("hello", actual.get(0).getValue(), "#6 - part 0 value");

        assertTrue(actual.get(1).isStaticFragment(), "#6 - part 1 static");
        assertEquals(" world", actual.get(1).getValue(), "#6 - part 1 value");
    }

    @Test
    void testCase7() {
        List<TemplateItemTwix> actual = TemplateTwix.getParsedTemplate("${{x}");
        assertEquals(1, actual.size(), "#7 - size");

        assertTrue(actual.getFirst().isStaticFragment(), "#7 - part 0 static");
        assertEquals("${{x}", actual.getFirst().getValue(), "#7 - part 0 value");
    }

    @Test
    void testCase8() {
        List<TemplateItemTwix> actual = TemplateTwix.getParsedTemplate("${\\{x}");
        assertEquals(1, actual.size(), "#8 - size");

        assertFalse(actual.getFirst().isStaticFragment(), "#8 - part 0 static");
        assertEquals("{x", actual.getFirst().getValue(), "#8 - part 0 value");
    }

    @Test
    void testCase9() {
        List<TemplateItemTwix> actual = TemplateTwix.getParsedTemplate("H ${abc\\\\x}");
        assertEquals(2, actual.size(), "#9 - size");

        assertTrue(actual.get(0).isStaticFragment(), "#9 - part 0 static");
        assertEquals("H ", actual.get(0).getValue(), "#9 - part 0 value");

        assertFalse(actual.get(1).isStaticFragment(), "#9 - part 1 static");
        assertEquals("abc\\x", actual.get(1).getValue(), "#9 - part 1 value");
    }

    @Test
    void testCase10() {
        List<TemplateItemTwix> actual = TemplateTwix.getParsedTemplate("H ${abc\\{x}");
        assertEquals(2, actual.size(), "#10 - size");

        assertTrue(actual.get(0).isStaticFragment(), "#10 - part 0 static");
        assertEquals("H ", actual.get(0).getValue(), "#10 - part 0 value");

        assertFalse(actual.get(1).isStaticFragment(), "#10 - part 1 static");
        assertEquals("abc{x", actual.get(1).getValue(), "#10 - part 1 value");
    }

    @Test
    void testCase11() {
        List<TemplateItemTwix> actual = TemplateTwix.getParsedTemplate("H ${abc\\}x}");
        assertEquals(2, actual.size(), "#11 - size");

        assertTrue(actual.get(0).isStaticFragment(), "#11 - part 0 static");
        assertEquals("H ", actual.get(0).getValue(), "#11 - part 0 value");

        assertFalse(actual.get(1).isStaticFragment(), "#11 - part 1 static");
        assertEquals("abc}x", actual.get(1).getValue(), "#11 - part 1 value");
    }

    @Test
    void testCase12() {
        List<TemplateItemTwix> actual = TemplateTwix.getParsedTemplate("H ${abc\\$\\{x}");
        assertEquals(2, actual.size(), "#12 - size");

        assertTrue(actual.get(0).isStaticFragment(), "#12 - part 0 static");
        assertEquals("H ", actual.get(0).getValue(), "#12 - part 0 value");

        assertFalse(actual.get(1).isStaticFragment(), "#12 - part 1 static");
        assertEquals("abc${x", actual.get(1).getValue(), "#12 - part 1 value");
    }

    @Test
    void testCase13() {
        List<TemplateItemTwix> actual = TemplateTwix.getParsedTemplate("$$$");
        assertEquals(1, actual.size(), "#13 - size");

        assertTrue(actual.getFirst().isStaticFragment(), "#13 - part 0 static");
        assertEquals("$$$", actual.getFirst().getValue(), "#13 - part 0 value");
    }

    @Test
    void testCase14() {
        List<TemplateItemTwix> actual = TemplateTwix.getParsedTemplate("$\\$$");
        assertEquals(1, actual.size(), "#14 - size");

        assertTrue(actual.getFirst().isStaticFragment(), "#14 - part 0 static");
        assertEquals("$\\$$", actual.getFirst().getValue(), "#14 - part 0 value");
    }

    @Test
    void testCase15() {
        List<TemplateItemTwix> actual = TemplateTwix.getParsedTemplate("${\\$}");
        assertEquals(1, actual.size(), "#15 - size");

        assertFalse(actual.getFirst().isStaticFragment(), "#15 - part 0 static");
        assertEquals("$", actual.getFirst().getValue(), "#15 - part 0 value");
    }

    @Test
    void testCase16() {
        List<TemplateItemTwix> actual = TemplateTwix.getParsedTemplate("Hello ${data.username}");
        assertEquals(2, actual.size(), "#16 - size");

        assertTrue(actual.get(0).isStaticFragment(), "#16 - part 0 static");
        assertEquals("Hello ", actual.get(0).getValue(), "#16 - part 0 value");

        assertFalse(actual.get(1).isStaticFragment(), "#16 - part 1 static");
        assertEquals("data.username", actual.get(1).getValue(), "#16 - part 1 value");
    }

    @Test
    void testCase17() {
        List<TemplateItemTwix> actual = TemplateTwix.getParsedTemplate("$${opa}");
        assertEquals(2, actual.size(), "#17 - size");

        assertTrue(actual.get(0).isStaticFragment(), "#17 - part 0 static");
        assertEquals("$", actual.get(0).getValue(), "#17 - part 0 value");

        assertFalse(actual.get(1).isStaticFragment(), "#17 - part 1 static");
        assertEquals("opa", actual.get(1).getValue(), "#17 - part 1 value");
    }

    @Test
    void testCase18() {
        List<TemplateItemTwix> actual = TemplateTwix.getParsedTemplate("$$${opa}");
        assertEquals(2, actual.size(), "#18 - size");

        assertTrue(actual.get(0).isStaticFragment(), "#18 - part 0 static");
        assertEquals("$$", actual.get(0).getValue(), "#18 - part 0 value");

        assertFalse(actual.get(1).isStaticFragment(), "#18 - part 1 static");
        assertEquals("opa", actual.get(1).getValue(), "#18 - part 1 value");
    }

    @Test
    void testCase19() {
        List<TemplateItemTwix> actual = TemplateTwix.getParsedTemplate("Hello ${opa} world");
        assertEquals(3, actual.size(), "#19 - size");

        assertTrue(actual.get(0).isStaticFragment(), "#19 - part 0 static");
        assertEquals("Hello ", actual.get(0).getValue(), "#19 - part 0 value");

        assertFalse(actual.get(1).isStaticFragment(), "#19 - part 1 static");
        assertEquals("opa", actual.get(1).getValue(), "#19 - part 1 value");

        assertTrue(actual.get(2).isStaticFragment(), "#19 - part 2 static");
        assertEquals(" world", actual.get(2).getValue(), "#19 - part 2 value");
    }

    @Test
    void testCase20() {
        List<TemplateItemTwix> actual = TemplateTwix.getParsedTemplate("${hello}_${world}");
        assertEquals(3, actual.size(), "#20 - size");

        assertFalse(actual.get(0).isStaticFragment(), "#20 - part 0 static");
        assertEquals("hello", actual.get(0).getValue(), "#20 - part 0 value");

        assertTrue(actual.get(1).isStaticFragment(), "#20 - part 1 static");
        assertEquals("_", actual.get(1).getValue(), "#20 - part 1 value");

        assertFalse(actual.get(2).isStaticFragment(), "#20 - part 2 static");
        assertEquals("world", actual.get(2).getValue(), "#20 - part 2 value");
    }

    @Test
    void testCase21() {
        List<TemplateItemTwix> actual = TemplateTwix.getParsedTemplate("${hello}${world}");
        assertEquals(2, actual.size(), "#21 - size");

        assertFalse(actual.get(0).isStaticFragment(), "#21 - part 0 static");
        assertEquals("hello", actual.get(0).getValue(), "#21 - part 0 value");

        assertFalse(actual.get(1).isStaticFragment(), "#21 - part 1 static");
        assertEquals("world", actual.get(1).getValue(), "#21 - part 1 value");
    }

    @Test
    void testCase22() {
        List<TemplateItemTwix> actual = TemplateTwix.getParsedTemplate("${hello}$$${world}");
        assertEquals(3, actual.size(), "#22 - size");

        assertFalse(actual.get(0).isStaticFragment(), "#22 - part 0 static");
        assertEquals("hello", actual.get(0).getValue(), "#22 - part 0 value");

        assertTrue(actual.get(1).isStaticFragment(), "#22 - part 1 static");
        assertEquals("$$", actual.get(1).getValue(), "#22 - part 1 value");

        assertFalse(actual.get(2).isStaticFragment(), "#22 - part 2 static");
        assertEquals("world", actual.get(2).getValue(), "#22 - part 2 value");
    }

    @Test
    void testCase23() {
        List<TemplateItemTwix> actual = TemplateTwix.getParsedTemplate("${hello}$\\$${world}");
        assertEquals(3, actual.size(), "#23 - size");

        assertFalse(actual.get(0).isStaticFragment(), "#23 - part 0 static");
        assertEquals("hello", actual.get(0).getValue(), "#23 - part 0 value");

        assertTrue(actual.get(1).isStaticFragment(), "#23 - part 1 static");
        assertEquals("$\\$", actual.get(1).getValue(), "#23 - part 1 value");

        assertFalse(actual.get(2).isStaticFragment(), "#23 - part 2 static");
        assertEquals("world", actual.get(2).getValue(), "#23 - part 2 value");
    }

    @Test
    void testCase24() {
        List<TemplateItemTwix> actual = TemplateTwix.getParsedTemplate("${h\\\\ello}$\\$${world}");
        assertEquals(3, actual.size(), "#24 - size");

        assertFalse(actual.get(0).isStaticFragment(), "#24 - part 0 static");
        assertEquals("h\\ello", actual.get(0).getValue(), "#24 - part 0 value");

        assertTrue(actual.get(1).isStaticFragment(), "#24 - part 1 static");
        assertEquals("$\\$", actual.get(1).getValue(), "#24 - part 1 value");

        assertFalse(actual.get(2).isStaticFragment(), "#24 - part 2 static");
        assertEquals("world", actual.get(2).getValue(), "#24 - part 2 value");
    }

    @Test
    void testCase25() {
        List<TemplateItemTwix> actual = TemplateTwix.getParsedTemplate("${fwe");
        assertEquals(1, actual.size(), "#25 - size");

        assertTrue(actual.getFirst().isStaticFragment(), "#25 - part 0 static");
        assertEquals("${fwe", actual.getFirst().getValue(), "#25 - part 0 value");
    }

}