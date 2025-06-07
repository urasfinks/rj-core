package ru.jamsys.core.flat.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.extension.UniversalPath;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilUriTest {

    @Test
    public void parseParameter() {
        UniversalPath universalPath = new UniversalPath("http://test.com/1.txt?x=y/e.pdf");
        Assertions.assertEquals("x=y/e.pdf", universalPath.getParameters());
        Assertions.assertEquals("{x=y/e.pdf}", universalPath.parseParameter().toString());
    }

    @Test
    void parseUri() {
        Map<String, List<String>> stringListMap = UtilUri.parseParameters("https://host.org/?x=y&a=1&a=2");
        Assertions.assertEquals("{x=[y], a=[1, 2]}", stringListMap.toString());
    }

    @Test
    void parseUriReduce() {
        Map<String, String> stringListMap = UtilUri.parseParameters("https://host.org/?x=y&a=1&a=2", strings -> String.join(", ", strings));
        Assertions.assertEquals("{x=y, a=1, 2}", stringListMap.toString());
        Assertions.assertEquals("{x=, a=1, 2}", UtilUri.parseParameters("https://host.org/?x=&a=1&a=2", strings -> String.join(", ", strings)).toString());
        Assertions.assertEquals("{x=, a=1}", UtilUri.parseParameters("https://host.org/?x=&a=1&a=2", List::getFirst).toString());
    }

    @Test
    void build() {
        Map<String, List<String>> stringListMap = UtilUri.parseParameters("https://host.org/?x=y&a=1&a=2");
        Assertions.assertEquals("/po/pt.html?x=y&a=1&a=2", UtilUri.build("/po/pt.html", stringListMap));

        Map<String, String> stringStringMap = UtilUri.parseParameters("https://host.org/?x=y&a=1&a=2", List::getFirst);
        Assertions.assertEquals("/po/pt.html?x=y&a=1", UtilUri.build("/po/pt.html", stringStringMap));
    }

    @Test
    public void testBuild_SingleParam() {
        Map<String, Object> params = Map.of("name", "Alice");
        String url = UtilUri.build("/api/test", params);
        assertEquals("/api/test?name=Alice", url);
    }

    @Test
    public void testBuild_MultipleParams() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("a", "1");
        params.put("b", "x y");
        params.put("c", Arrays.asList("v1", "v2"));

        String url = UtilUri.build("/do", params);
        assertTrue(url.startsWith("/do?"));
        assertTrue(url.contains("a=1"));
        assertTrue(url.contains("b=x+y"));  // space encoded as +
        assertTrue(url.contains("c=v1"));
        assertTrue(url.contains("c=v2"));
    }

    @Test
    public void testBuild_EmptyParams() {
        String url = UtilUri.build("/home", Collections.emptyMap());
        assertEquals("/home", url);
    }

    @Test
    public void testParseParameters_ListVariant() {
        String uri = "/test?x=1&x=2&y=3";
        Map<String, List<String>> params = UtilUri.parseParameters(uri);
        assertEquals(List.of("1", "2"), params.get("x"));
        assertEquals(List.of("3"), params.get("y"));
    }

    @Test
    public void testParseParameters_ReducedVariant() {
        String uri = "/test?x=1&x=2&y=3";
        Map<String, String> params = UtilUri.parseParameters(uri, list -> String.join(",", list));
        assertEquals("1,2", params.get("x"));
        assertEquals("3", params.get("y"));
    }

    @Test
    public void testEncode() throws Exception {
        assertEquals("a%2Fb+c", UtilUri.encode("a/b c", "UTF-8"));
    }

    @Test
    public void testDecode() throws Exception {
        assertEquals("a/b c", UtilUri.decode("a%2Fb+c", "UTF-8"));
    }

    @Test
    public void testEncodeDecodeConsistency() throws Exception {
        String original = "param=value&key=x y/z";
        String encoded = UtilUri.encode(original);
        String decoded = UtilUri.decode(encoded);
        assertEquals(original, decoded);
    }

}