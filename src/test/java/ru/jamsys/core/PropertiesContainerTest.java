package ru.jamsys.core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.property.PropertiesContainer;
import ru.jamsys.core.extension.property.Property;

import java.util.concurrent.atomic.AtomicInteger;

class PropertiesContainerTest {

    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runCore();
        App.get(ServiceProperty.class).setProperty("run.args.IgnoreClassFinder.test1", "true");
        App.get(ServiceProperty.class).setProperty("run.args.IgnoreClassFinder.test2", "false");
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @Test
    void unwatch() {
        PropertiesContainer container = App
                .get(ServiceProperty.class)
                .getFactory()
                .getContainer();
        AtomicInteger x = new AtomicInteger(0);
        container.watch(String.class, "run.args.IgnoreClassFinder.test1", null, true, _ -> x.incrementAndGet());
        Property<String> p1 = container.watch(String.class, "run.args.IgnoreClassFinder.test2", null, true, _ -> x.incrementAndGet());
        Assertions.assertEquals(2, x.get());
        p1.set("1");
        Assertions.assertEquals(3, x.get());
        container.unwatch("run.args.IgnoreClassFinder.test2");
        Assertions.assertEquals(3, x.get());
        p1.set("2");
        Assertions.assertEquals(3, x.get());
    }
}