package ru.jamsys.core.extension.property;

import org.junit.jupiter.api.*;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;

import java.util.concurrent.atomic.AtomicInteger;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PropertiesAgentTest {

    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runCore();
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @Test
    @Order(1)
    public void collection() {
        PropertiesAgent map = App
                .get(ServiceProperty.class)
                .getFactory()
                .getPropertiesAgentMap(null, "run.args.IgnoreClassFinder", false);

        Assertions.assertEquals("[test1, test2]", map.getRepositoryProperties().toString());
        Assertions.assertEquals("[run.args.IgnoreClassFinder.test1, run.args.IgnoreClassFinder.test2]", map.getServiceProperties().toString());

        Assertions.assertEquals("{test1=true, test2=false}", map.getPropertiesRepository().getProperties().toString());

    }

    @Test
    @Order(2)
    public void onUpdate() {
        AtomicInteger x = new AtomicInteger(0);
        App
                .get(ServiceProperty.class)
                .getFactory()
                .getPropertiesAgentMap(
                        _ -> x.incrementAndGet(),
                        "run.args.IgnoreClassFinder",
                        false
                );
        Assertions.assertEquals(2, x.get());

        Property<Boolean> test1 = App.get(ServiceProperty.class).getFactory()
                .getProperty(
                        Boolean.class,
                        "run.args.IgnoreClassFinder.test1",
                        true,
                        true,
                        null
                );

        test1.set(false);
        Assertions.assertEquals(3, x.get());
        test1.set(true);
    }

    @Test
    @Order(3)
    public void singlePropertyRequire() {
        AtomicInteger x = new AtomicInteger(0);
        Property<String> prop = App
                .get(ServiceProperty.class)
                .getFactory()
                .getProperty(
                        String.class,
                        "run.args.IgnoreClassFinder.test1",
                        "xx",
                        true,
                        _ -> x.incrementAndGet()
                );
        Assertions.assertEquals("true", prop.get());
        App
                .get(ServiceProperty.class)
                .setProperty("run.args.IgnoreClassFinder.test1", "false");
        Assertions.assertEquals("false", prop.get());
        prop.set("true");
        Assertions.assertEquals("true", prop.get());
        Assertions.assertEquals(3, x.get());
    }

    @Test
    @Order(4)
    public void singlePropertyRequireException() {
        AtomicInteger x = new AtomicInteger(0);
        try {
            App
                    .get(ServiceProperty.class)
                    .getFactory()
                    .getProperty(
                            String.class,
                            "run.args.IgnoreClassFinder.test3",
                            "xx",
                            true,
                            _ -> x.incrementAndGet()
                    );
            Assertions.fail();
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    @Test
    @Order(5)
    public void singlePropertyNotRequire() {
        AtomicInteger x = new AtomicInteger(0);
        Property<String> prop = App
                .get(ServiceProperty.class)
                .getFactory()
                .getProperty(
                        String.class,
                        "run.args.IgnoreClassFinder.test3",
                        "xx",
                        false,
                        _ -> x.incrementAndGet()
                );
        Assertions.assertEquals("xx", prop.get());
    }

}