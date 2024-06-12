package ru.jamsys.core.component;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.flat.util.UtilFile;

import static org.junit.jupiter.api.Assertions.assertNull;

class SecurityComponentTest {

    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        App.run(args);
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @Test
    void get() throws Exception {
        SecurityComponent securityComponent = App.get(SecurityComponent.class);
        securityComponent.setPathStorage("unit-test.jks");
        char[] password = "12345".toCharArray();
        securityComponent.loadKeyStorage(password);
        securityComponent.add("test", "12345".toCharArray(), password);
        Assertions.assertEquals("12345", new String(securityComponent.get("test")), "#1");
        securityComponent.add("test", "123456".toCharArray(), password);
        Assertions.assertEquals("123456", new String(securityComponent.get("test")), "#2");
        securityComponent.remove("test", password);
        assertNull(securityComponent.get("test"), "#3");
        securityComponent.add("test2", "123456".toCharArray(), password);
        Assertions.assertEquals("[test2]", securityComponent.getAvailableAliases().toString(), "#4");
        UtilFile.removeIfExist("unit-test.jks");
    }

}