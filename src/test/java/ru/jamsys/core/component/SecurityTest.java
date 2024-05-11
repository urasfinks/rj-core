package ru.jamsys.core.component;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.util.UtilFile;

import static org.junit.jupiter.api.Assertions.assertNull;

class SecurityTest {

    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        App.main(args);
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @Test
    void get() throws Exception {
        Security security = App.context.getBean(Security.class);
        security.setPathStorage("unit-test.jks");
        char[] password = "12345".toCharArray();
        security.loadKeyStorage(password);
        security.add("test", "12345".toCharArray(), password);
        Assertions.assertEquals("12345", new String(security.get("test")), "#1");
        security.add("test", "123456".toCharArray(), password);
        Assertions.assertEquals("123456", new String(security.get("test")), "#2");
        security.remove("test", password);
        assertNull(security.get("test"), "#3");
        security.add("test2", "123456".toCharArray(), password);
        Assertions.assertEquals("[test2]", security.getAvailableAliases().toString(), "#4");
        UtilFile.removeIfExist("unit-test.jks");
    }

}