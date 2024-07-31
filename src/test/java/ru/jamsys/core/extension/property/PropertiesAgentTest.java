package ru.jamsys.core.extension.property;

import org.junit.jupiter.api.*;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.property.repository.PropertiesRepositoryMap;
import ru.jamsys.core.flat.util.Util;

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
        PropertiesRepositoryMap propertiesRepositoryMap = new PropertiesRepositoryMap();
        PropertiesAgent propertiesAgent = App
                .get(ServiceProperty.class)
                .getFactory()
                .getPropertiesAgent(null, propertiesRepositoryMap, "run.args.IgnoreClassFinder", false);

        propertiesAgent.series("run\\.args\\.IgnoreClassFinder.*");

        Assertions.assertEquals("{test1=true, test2=false}", propertiesRepositoryMap.getProperties().toString());
        Assertions.assertEquals("[run\\.args\\.IgnoreClassFinder.*]", propertiesAgent.getRepositoryPropertyListeners().toString());

        // Дребидень полная в случае с regexp
        Assertions.assertEquals("[run.args.IgnoreClassFinder.run\\.args\\.IgnoreClassFinder.*]", propertiesAgent.getServicePropertyListeners().toString());

        Assertions.assertEquals("{test1=true, test2=false}", propertiesAgent.getPropertiesRepository().getProperties().toString());

        App.get(ServiceProperty.class).setProperty("run.args.IgnoreClassFinder.test1", "false");

        Assertions.assertEquals("{test1=false, test2=false}", propertiesAgent.getPropertiesRepository().getProperties().toString());

        Assertions.assertEquals("false", App.get(ServiceProperty.class).getProp().get("run.args.IgnoreClassFinder.test2").getValue());

        propertiesAgent.setPropertyRepository("test2", "true");

        Assertions.assertEquals("{test1=false, test2=true}", propertiesAgent.getPropertiesRepository().getProperties().toString());

        Assertions.assertEquals("true", App.get(ServiceProperty.class).getProp().get("run.args.IgnoreClassFinder.test2").getValue());

    }

    @Test
    @Order(2)
    public void onUpdate() {
        PropertiesRepositoryMap propertiesRepositoryMap = new PropertiesRepositoryMap();
        AtomicInteger x = new AtomicInteger(0);
        PropertiesAgent propertiesAgent = App
                .get(ServiceProperty.class)
                .getFactory()
                .getPropertiesAgent(
                        mapAlias -> x.addAndGet(mapAlias.size()),
                        propertiesRepositoryMap,
                        "run.args.IgnoreClassFinder",
                        false
                );

        propertiesAgent.series("run\\.args\\.IgnoreClassFinder.*");

        Assertions.assertEquals(2, x.get());

        Property<Boolean> test1 = App.get(ServiceProperty.class).getFactory()
                .getProperty(
                        Boolean.class,
                        "run.args.IgnoreClassFinder.test1",
                        true,
                        true,
                        null
                );

        test1.set(true);
        Assertions.assertEquals(3, x.get());

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

    @Test
    @Order(6)
    public void simpleUpdate() {

        PropertiesRepositoryMap propertiesRepositoryMap = new PropertiesRepositoryMap();
        AtomicInteger x = new AtomicInteger(0);
        PropertiesAgent propertiesAgent = App
                .get(ServiceProperty.class)
                .getFactory()
                .getPropertiesAgent(
                        null,
                        propertiesRepositoryMap,
                        null,
                        true
                );
        propertiesAgent.add(
                Boolean.class,
                "run.args.IgnoreClassFinder.test1",
                true,
                true,
                aBoolean -> {
                    Util.logConsole(aBoolean.toString());
                    x.incrementAndGet();
                });
        Assertions.assertEquals(1, x.get());

        propertiesAgent.setPropertyRepository("run.args.IgnoreClassFinder.test1", "false");

        Assertions.assertEquals(2, x.get());

        Assertions.assertEquals("{run.args.IgnoreClassFinder.test1=false}", propertiesAgent.getPropertiesRepository().getProperties().toString());

        Property<Boolean> prop = App
                .get(ServiceProperty.class)
                .getFactory()
                .getProperty(
                        Boolean.class,
                        "run.args.IgnoreClassFinder.test1",
                        true,
                        true,
                        null
                );
        Assertions.assertEquals(false, prop.get());

        Assertions.assertEquals(2, x.get());

        prop.set(true);

        Assertions.assertEquals(true, prop.get());

        Assertions.assertEquals(3, x.get());

    }

}