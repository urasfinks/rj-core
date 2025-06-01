package ru.jamsys.core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.flat.util.UtilFile;

import static org.junit.jupiter.api.Assertions.assertNull;

// IO time: 203ms
// COMPUTE time: 202ms

class SecurityComponentTest {

    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runCore();
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @Test
    void get() throws Exception {
        SecurityComponent securityComponent = App.get(SecurityComponent.class);
        securityComponent.getProperty().setPathStorage("unit-test.jks");
        char[] password = "12345".toCharArray();
        securityComponent.loadKeyStorage(password);
        securityComponent.add("test", "12345".toCharArray(), password);
        Assertions.assertEquals("12345", new String(securityComponent.get("test")), "#1");
        securityComponent.add("test", "123456".toCharArray(), password);
        Assertions.assertEquals("123456", new String(securityComponent.get("test")), "#2");
        securityComponent.remove("test", password);
        Assertions.assertThrows(RuntimeException.class, () -> securityComponent.get("test"), "#3");
        securityComponent.add("test2", "123456".toCharArray(), password);
        Assertions.assertEquals("[test2]", securityComponent.getAvailableAliases().toString(), "#4");
        UtilFile.removeIfExist("unit-test.jks");
    }

}