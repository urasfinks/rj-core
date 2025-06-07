package ru.jamsys.core.extension.file;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.extension.UniversalPath;
import ru.jamsys.core.flat.util.UtilJson;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UniversalPathTest {

    @Test
    void testFilePath() {
        Assertions.assertEquals("""
                        {"path":"3/2/1.txt","folder":"3/2","fileName":"1.txt","extension":"txt","parameters":null}""",
                UtilJson.toString(new UniversalPath("3/2/1.txt"), "{}")
        );
        Assertions.assertEquals("""
                        {"path":"2/1.txt","folder":"2","fileName":"1.txt","extension":"txt","parameters":null}""",
                UtilJson.toString(new UniversalPath("2/1.txt"), "{}")
        );
        Assertions.assertEquals("""
                        {"path":"1.txt","folder":null,"fileName":"1.txt","extension":"txt","parameters":null}""",
                UtilJson.toString(new UniversalPath("2/../1.txt"), "{}")
        );
        Assertions.assertThrowsExactly(RuntimeException.class,
                () -> UtilJson.toString(new UniversalPath("../1.txt"), "{}")
        );
        Assertions.assertEquals("""
                        {"path":"/1.txt","folder":null,"fileName":"1.txt","extension":"txt","parameters":null}""",
                UtilJson.toString(new UniversalPath("/1/2/../../1.txt"), "{}")
        );
        Assertions.assertEquals("""
                        {"path":"/1/1.txt","folder":"/1","fileName":"1.txt","extension":"txt","parameters":null}""",
                UtilJson.toString(new UniversalPath("/1/2/../1.txt"), "{}")
        );
        Assertions.assertEquals("""
                        {"path":"/1.txt","folder":null,"fileName":"1.txt","extension":"txt","parameters":null}""",
                UtilJson.toString(new UniversalPath("/test/../1.txt"), "{}")
        );
        Assertions.assertEquals("""
                        {"path":"http://test.com/1.txt","folder":"http://test.com","fileName":"1.txt","extension":"txt","parameters":"x=y/e.pdf"}""",
                UtilJson.toString(new UniversalPath("http://test.com/1.txt?x=y/e.pdf"), "{}")
        );
        Assertions.assertEquals("""
                        {"path":"http://test.com/1.txt","folder":"http://test.com","fileName":"1.txt","extension":"txt","parameters":""}""",
                UtilJson.toString(new UniversalPath("http://test.com/1.txt?"), "{}")
        );
        Assertions.assertEquals("""
                        {"path":"http://test.com/1.txt","folder":"http://test.com","fileName":"1.txt","extension":"txt","parameters":null}""",
                UtilJson.toString(new UniversalPath("http://test.com/1.txt"), "{}")
        );
        Assertions.assertEquals("""
                        {"path":"/test/1","folder":"/test","fileName":"1","extension":null,"parameters":null}""",
                UtilJson.toString(new UniversalPath("/test/1"), "{}")
        );
        Assertions.assertEquals("""
                        {"path":"/test/1.","folder":"/test","fileName":"1.","extension":null,"parameters":null}""",
                UtilJson.toString(new UniversalPath("/test/1."), "{}")
        );
        Assertions.assertEquals("""
                        {"path":"/1","folder":null,"fileName":"1","extension":null,"parameters":null}""",
                UtilJson.toString(new UniversalPath("/1"), "{}")
        );
        Assertions.assertEquals("""
                        {"path":"1","folder":null,"fileName":"1","extension":null,"parameters":null}""",
                UtilJson.toString(new UniversalPath("1"), "{}")
        );

    }

    @Test
    void getExtension() {
        Assertions.assertEquals("txt", new UniversalPath("/test/1.txt").getExtension());
        Assertions.assertEquals("txt", new UniversalPath("//test//1.txt").getExtension());
        Assertions.assertEquals("txt", new UniversalPath("\test\1.txt").getExtension());
        Assertions.assertEquals("txt", new UniversalPath("\\test\\1.txt").getExtension());
        Assertions.assertEquals("txt", new UniversalPath("http://test.com/1.txt").getExtension());
        Assertions.assertEquals("txt", new UniversalPath("http://test.com/1.txt?x=y").getExtension());
        Assertions.assertEquals("txt", new UniversalPath("http://test.com/1.txt?x=y/e.pdf").getExtension());
    }

    @Test
    void path() {
        UniversalPath universalPath = new UniversalPath("/?x=y");
        Assertions.assertEquals("{path=/, folder=null, fileName=null, extension=null, parameters=x=y}", universalPath.getJsonValue().toString());
        Assertions.assertEquals("{x=y}", universalPath.parseParameter().toString());
    }

    @Test
    public void testSimplePath() {
        UniversalPath fp = new UniversalPath("folder/file.txt");

        assertEquals("folder/file.txt", fp.getPath());
        assertEquals("folder", fp.getFolder());
        assertEquals("file.txt", fp.getFileName());
        assertEquals("txt", fp.getExtension());
        assertNull(fp.getParameters());
    }

    @Test
    public void testPathWithParameters() {
        UniversalPath fp = new UniversalPath("folder/file.txt?version=1&token=abc");

        assertEquals("folder/file.txt", fp.getPath());
        assertEquals("folder", fp.getFolder());
        assertEquals("file.txt", fp.getFileName());
        assertEquals("txt", fp.getExtension());
        assertEquals("version=1&token=abc", fp.getParameters());

        Map<String, String> paramMap = fp.parseParameter();
        assertEquals("1", paramMap.get("version"));
        assertEquals("abc", paramMap.get("token"));
    }

    @Test
    public void testPathWithNoExtension() {
        UniversalPath fp = new UniversalPath("folder/file");

        assertEquals("folder/file", fp.getPath());
        assertEquals("folder", fp.getFolder());
        assertEquals("file", fp.getFileName());
        assertNull(fp.getExtension());
    }

    @Test
    public void testSingleFileInRoot() {
        UniversalPath fp = new UniversalPath("/file.tar.gz");

        assertEquals("/file.tar.gz", fp.getPath());
        assertNull(fp.getFolder());
        assertEquals("file.tar.gz", fp.getFileName());
        assertEquals("gz", fp.getExtension());
    }

    @Test
    public void testDotDotNavigation() {
        UniversalPath fp = new UniversalPath("a/b/c/../d/file.log");

        assertEquals("a/b/d/file.log", fp.getPath());
        assertEquals("a/b/d", fp.getFolder());
        assertEquals("file.log", fp.getFileName());
        assertEquals("log", fp.getExtension());
    }

    @Test
    public void testRootPathOnly() {
        UniversalPath fp = new UniversalPath("/");

        assertEquals("/", fp.getPath());
        assertNull(fp.getFolder());
        assertNull(fp.getFileName());
        assertNull(fp.getExtension());
    }

    @Test
    public void testEmptyPathThrows() {
        Exception ex = assertThrows(RuntimeException.class, () -> new UniversalPath(null));
        assertEquals("inputPath is null", ex.getMessage());
    }

    @Test
    public void testTooManyDotDots() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> new UniversalPath(".."));
        assertTrue(ex.getMessage().contains("Exception remove .. from path"));
    }

    @Test
    public void testGetJsonValue() {
        UniversalPath fp = new UniversalPath("dir/file.jpg?mode=edit");

        var json = fp.getJsonValue();

        assertEquals("dir/file.jpg", json.get("path"));
        assertEquals("dir", json.get("folder"));
        assertEquals("file.jpg", json.get("fileName"));
        assertEquals("jpg", json.get("extension"));
        assertEquals("mode=edit", json.get("parameters"));
    }

    @Test
    void getPath() {
        Assertions.assertEquals("/po/pt.html", new UniversalPath("https://host.org/po/pt.html?x=y&a=1&a=2").getPathWithoutProtocol());
        Assertions.assertEquals("/po/pt.html", new UniversalPath("https://host.org/po/pt.html/?x=y&a=1&a=2").getPathWithoutProtocol());
        Assertions.assertEquals("/po", new UniversalPath("https://host.org/po/?x=y&a=1&a=2").getPathWithoutProtocol());
        Assertions.assertEquals("host.org/po", new UniversalPath("host.org/po/?x=y&a=1&a=2").getPathWithoutProtocol());
    }

    @Test
    void test() {
        Assertions.assertEquals("host.org/po", new UniversalPath("host.org/po/?x=y&a=1&a=2").getPathWithoutProtocol());
    }

    @Test
    void testWithHttpSchemaAndPath() {
        UniversalPath wrapper = new UniversalPath("https://host.org/po/pt.html");
        assertEquals("/po/pt.html", wrapper.getPathWithoutProtocol());
    }

    @Test
    void testWithHttpSchemaNoPath() {
        UniversalPath wrapper = new UniversalPath("http://localhost:8080");
        assertEquals("/", wrapper.getPathWithoutProtocol());
    }

    @Test
    void testWithWsSchemaAndQuery() {
        UniversalPath wrapper = new UniversalPath("ws://example.com/ws/api?v=2");
        assertEquals("/ws/api", wrapper.getPathWithoutProtocol());
    }

    @Test
    void testRelativePathOnly() {
        UniversalPath wrapper = new UniversalPath("/api/data");
        assertEquals("/api/data", wrapper.getPathWithoutProtocol());
    }

    @Test
    void testHostWithoutSchema() {
        UniversalPath wrapper = new UniversalPath("host.org/po/pt.html");
        assertEquals("host.org/po/pt.html", wrapper.getPathWithoutProtocol());
    }

    @Test
    void testHostPortWithoutSchema() {
        UniversalPath wrapper = new UniversalPath("localhost:3000/hello");
        assertEquals("localhost:3000/hello", wrapper.getPathWithoutProtocol());
    }

    @Test
    void testNullPath() {
        Assertions.assertThrows(RuntimeException.class, () -> new UniversalPath(null));
    }

    @Test
    void testEmptyPath() {
        UniversalPath wrapper = new UniversalPath("");
        assertEquals("", wrapper.getPathWithoutProtocol());
    }

}