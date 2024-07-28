package ru.jamsys.core.extension.property;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;

import java.util.concurrent.atomic.AtomicInteger;

class PropertiesContainerTest {

    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runCore();
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
        container.watch(String.class, "run.args.IgnoreClassFinder.test1", null, (_, _) -> x.incrementAndGet());
        Property<String> p1 = container.watch(String.class, "run.args.IgnoreClassFinder.test2", null, (_, _) -> x.incrementAndGet());
        Assertions.assertEquals(2, x.get());
        p1.set("1");
        Assertions.assertEquals(3, x.get());
        container.unwatch("run.args.IgnoreClassFinder.test2");
        Assertions.assertEquals(3, x.get());
        p1.set("2");
        Assertions.assertEquals(3, x.get());
    }
}