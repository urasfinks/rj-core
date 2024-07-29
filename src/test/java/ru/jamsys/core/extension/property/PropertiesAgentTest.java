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
                .getPropertiesAgent("run.args.IgnoreClassFinder", false, _ -> {});

        Assertions.assertEquals("[test1, test2]", map.getKeySetRelative().toString());
        Assertions.assertEquals("[run.args.IgnoreClassFinder.test1, run.args.IgnoreClassFinder.test2]", map.getKeySetAbsolute().toString());
        Assertions.assertEquals("true", map.getPropertyRelative(String.class, "test1").get());
        Assertions.assertEquals("false", map.getPropertyRelative(String.class, "test2").get());
        Assertions.assertEquals("true", map.getPropertyAbsolute(String.class, "run.args.IgnoreClassFinder.test1").get());
        Assertions.assertEquals("false", map.getPropertyAbsolute(String.class, "run.args.IgnoreClassFinder.test2").get());
    }

    @Test
    @Order(2)
    public void onUpdate() {
        AtomicInteger x = new AtomicInteger(0);
        PropertiesAgent map = App
                .get(ServiceProperty.class)
                .getFactory()
                .getNsAgent(
                        "run.args.IgnoreClassFinder",
                        false,
                        _ -> x.incrementAndGet()
                );
        Assertions.assertEquals(2, x.get());
        PropertyNs<Boolean> test1 = map.getPropertyRelative(Boolean.class, "test1");
        test1.set(false);
        Assertions.assertEquals(3, x.get());
        test1.set(true);
    }

    @Test
    @Order(3)
    public void singlePropertyNsRequire() {
        AtomicInteger x = new AtomicInteger(0);
        PropertyNs<String> prop = App
                .get(ServiceProperty.class)
                .getFactory()
                .getPropertyNs(
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
    public void singlePropertyNsRequireException() {
        AtomicInteger x = new AtomicInteger(0);
        try {
            App
                    .get(ServiceProperty.class)
                    .getFactory()
                    .getPropertyNs(
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
    public void singlePropertyNsNotRequire() {
        AtomicInteger x = new AtomicInteger(0);
        PropertyNs<String> prop = App
                .get(ServiceProperty.class)
                .getFactory()
                .getPropertyNs(
                        String.class,
                        "run.args.IgnoreClassFinder.test3",
                        "xx",
                        false,
                        _ -> x.incrementAndGet()
                );
        Assertions.assertEquals("xx", prop.get());
    }

}