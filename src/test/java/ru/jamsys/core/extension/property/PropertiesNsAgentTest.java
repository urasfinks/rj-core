package ru.jamsys.core.extension.property;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;

import java.util.concurrent.atomic.AtomicInteger;

class PropertiesNsAgentTest {

    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runCore();
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @Test
    public void collection() {
        PropertiesNsAgent map = App
                .get(ServiceProperty.class)
                .getFactory()
                .getNsAgent("run.args.IgnoreClassFinder", false, _ -> {});

        Assertions.assertEquals("[test1, test2]", map.getKeySetWithoutNs().toString());
        Assertions.assertEquals("[run.args.IgnoreClassFinder.test1, run.args.IgnoreClassFinder.test2]", map.getKeySet().toString());
        Assertions.assertEquals("true", map.getWithoutNs(String.class, "test1").get());
        Assertions.assertEquals("false", map.getWithoutNs(String.class, "test2").get());
        Assertions.assertEquals("true", map.get(String.class, "run.args.IgnoreClassFinder.test1").get());
        Assertions.assertEquals("false", map.get(String.class, "run.args.IgnoreClassFinder.test2").get());
    }

    @Test
    public void onUpdate() {
        AtomicInteger x = new AtomicInteger(0);
        PropertiesNsAgent map = App
                .get(ServiceProperty.class)
                .getFactory()
                .getNsAgent(
                        "run.args.IgnoreClassFinder",
                        false,
                        _ -> x.incrementAndGet()
                );
        Assertions.assertEquals(2, x.get());
        PropertyNs<Boolean> test1 = map.getWithoutNs(Boolean.class, "test1");
        test1.set(false);
        Assertions.assertEquals(3, x.get());
    }

}