package ru.jamsys.core.extension.property;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;

import java.util.concurrent.atomic.AtomicInteger;

class PropertiesMapTest {

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
        PropertiesMap<String> map = App.get(ServiceProperty.class).getFactory().getMap("run.args.IgnoreClassFinder", String.class, null);
        Assertions.assertEquals("[test1, test2]", map.getKeySetWithoutNs().toString());
        Assertions.assertEquals("[run.args.IgnoreClassFinder.test1, run.args.IgnoreClassFinder.test2]", map.getKeySet().toString());
        Assertions.assertEquals("true", map.getWithoutNs("test1").get());
        Assertions.assertEquals("false", map.getWithoutNs("test2").get());
        Assertions.assertEquals("true", map.get("run.args.IgnoreClassFinder.test1").get());
        Assertions.assertEquals("false", map.get("run.args.IgnoreClassFinder.test2").get());
    }

    @Test
    public void onUpdate() {
        AtomicInteger x = new AtomicInteger(0);
        PropertiesMap<Boolean> map = App
                .get(ServiceProperty.class)
                .getFactory()
                .getMap(
                        "run.args.IgnoreClassFinder",
                        Boolean.class,
                        x::incrementAndGet
                );
        Assertions.assertEquals(2, x.get());
        Property<Boolean> test1 = map.getWithoutNs("test1");
        test1.set(false);
        Assertions.assertEquals(3, x.get());
    }

}