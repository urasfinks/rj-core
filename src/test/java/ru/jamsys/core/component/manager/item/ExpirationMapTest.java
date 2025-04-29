package ru.jamsys.core.component.manager.item;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.flat.util.Util;

class ExpirationMapTest {

    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runCore();
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    public static class XTest {

    }


    @Test
    void computeIfAbsent() {
        @SuppressWarnings("all")
        Manager.Configuration<ExpirationMap> expirationMapConfiguration = App.get(Manager.class).configure(
                ExpirationMap.class,
                "test",
                s -> new ExpirationMap<Integer, XTest>(s, 1000)
        );
        //ExpirationMap<Integer, XTest> test = App.get(ManagerExpirationMap.class).get("test", 1000);
        ExpirationMap<Integer, XTest> test = expirationMapConfiguration.get();
        XTest s = test.computeIfAbsent(10, _ -> new XTest());

        Assertions.assertEquals(1, test.size());
        Assertions.assertEquals(1, test.getExpirationMap().size());
        Assertions.assertEquals(s.hashCode(), test.get(10).hashCode());
        Assertions.assertEquals(s.hashCode(), test.get(10).hashCode());
        Util.testSleepMs(2000);
        Assertions.assertEquals(0, test.size());
        Assertions.assertEquals(0, test.getExpirationMap().size());
    }
}