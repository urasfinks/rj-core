package ru.jamsys.core.flat.util;

import org.junit.jupiter.api.Test;
import ru.jamsys.core.extension.exception.ForwardException;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class UtilSelectorTest {

    private final Map<String, Object> sample = Map.of(
            "user", Map.of(
                    "profile", Map.of(
                            "age", 30,
                            "name", "Alice",
                            "tags", List.of("admin", "dev")
                    )
            )
    );

    @Test
    public void testSimpleValue() {
        Integer age = UtilSelector.selector(sample, "user.profile.age");
        assertEquals(30, age);
    }

    @Test
    public void testStringValue() {
        String name = UtilSelector.selector(sample, "user.profile.name");
        assertEquals("Alice", name);
    }

    @Test
    public void testListValue() {
        List<String> tags = UtilSelector.selector(sample, "user.profile.tags");
        assertEquals(List.of("admin", "dev"), tags);
    }

    @Test
    public void testMissingKey() {
        ForwardException ex = assertThrows(ForwardException.class, () ->
                UtilSelector.selector(sample, "user.profile.unknown")
        );
        assertTrue(ex.getMessage().contains("Map does not contain key"));
    }

    @Test
    public void testPathTooDeep() {
        ForwardException ex = assertThrows(ForwardException.class, () ->
                UtilSelector.selector(sample, "user.profile.age.value")
        );
        UtilLog.printError(ex.getMessage());
        assertTrue(ex.getMessage().contains("Current object is neither Map nor List"));
    }

    @Test
    public void testInvalidRootSelector() {
        ForwardException ex = assertThrows(ForwardException.class, () ->
                UtilSelector.selector(sample, "invalid.key")
        );
        assertTrue(ex.getMessage().contains("Map does not contain key"));
    }

    @Test
    public void testSelectorReturnsNull() {
        Map<String, Object> inner = new HashMap<>();
        inner.put("y", null);

        Map<String, Object> withNull = new HashMap<>();
        withNull.put("x", inner);

        Object result = UtilSelector.selector(withNull, "x.y");
        assertNull(result);
    }

    @Test
    public void testSelectorOnEmptyPath() {
        assertThrows(ForwardException.class, () ->
                UtilSelector.selector(sample, "")
        );
    }
}
