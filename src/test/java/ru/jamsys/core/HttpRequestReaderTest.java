package ru.jamsys.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.extension.http.ServletRequestReader;
import ru.jamsys.core.extension.http.ServletResponseWriter;

import java.util.List;
import java.util.Map;

class HttpRequestReaderTest {

    @Test
    public void testMain() {
        try {
            ServletRequestReader.basicAuthHandler("Basic YWRtaW46cXdlcnR5", (user, password) -> {
                Assertions.assertEquals("admin", user);
                Assertions.assertEquals("qwerty", password);
            });
        } catch (Throwable th) {
            th.printStackTrace();
            Assertions.fail();
        }
        try {
            ServletRequestReader.basicAuthHandler("BasicYWRtaW46cXdlcnR5", (_, _) -> {});
            Assertions.fail();
        } catch (Throwable th) {
            Assertions.assertEquals(th.getMessage(), "Authorization header is not Basic");
        }
        try {
            ServletRequestReader.basicAuthHandler("Basic YWRtaW5xd2VydHk=", (_, _) -> {});
            Assertions.fail();
        } catch (Throwable th) {
            Assertions.assertEquals(th.getMessage(), "Error parsing");
        }
        try {
            ServletRequestReader.basicAuthHandler("Basic YWRtaW5xd2Vyd", (_, _) -> {});
            throw new RuntimeException("NO");
        } catch (Throwable th) {
            Assertions.assertNotEquals(th.getMessage(), "NO");
        }
    }

    @Test
    void parseUri(){
        Map<String, List<String>> stringListMap = ServletRequestReader.parseUriParameters("https://host.org/?x=y&a=1&a=2");
        Assertions.assertEquals("{x=[y], a=[1, 2]}", stringListMap.toString());
    }

    @Test
    void parseUriReduce(){
        Map<String, String> stringListMap = ServletRequestReader.parseUriParameters("https://host.org/?x=y&a=1&a=2", strings -> String.join(", ",strings));
        Assertions.assertEquals("{x=y, a=1, 2}", stringListMap.toString());
        Assertions.assertEquals("{x=, a=1, 2}", ServletRequestReader.parseUriParameters("https://host.org/?x=&a=1&a=2", strings -> String.join(", ",strings)).toString());
        Assertions.assertEquals("{x=, a=1}", ServletRequestReader.parseUriParameters("https://host.org/?x=&a=1&a=2", List::getFirst).toString());
    }

    @Test
    void getPath(){
        Assertions.assertEquals("/po/pt.html", ServletRequestReader.getPath("https://host.org/po/pt.html?x=y&a=1&a=2"));
        Assertions.assertEquals("/po/pt.html", ServletRequestReader.getPath("https://host.org/po/pt.html/?x=y&a=1&a=2"));
        Assertions.assertEquals("/po", ServletRequestReader.getPath("https://host.org/po/?x=y&a=1&a=2"));
    }

    @Test
    void buildUrlQuery(){
        Map<String, List<String>> stringListMap = ServletRequestReader.parseUriParameters("https://host.org/?x=y&a=1&a=2");
        Assertions.assertEquals("/po/pt.html?x=y&a=1&a=2", ServletResponseWriter.buildUrlQuery("/po/pt.html", stringListMap));
    }

}