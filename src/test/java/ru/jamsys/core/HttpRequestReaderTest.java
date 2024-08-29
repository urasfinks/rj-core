package ru.jamsys.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.extension.http.ServletRequestReader;

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

}