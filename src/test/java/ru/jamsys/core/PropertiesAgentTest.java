package ru.jamsys.core;

import lombok.Getter;
import org.junit.jupiter.api.*;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.annotation.PropertyName;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.property.PropertiesAgent;
import ru.jamsys.core.extension.property.Property;
import ru.jamsys.core.extension.property.repository.RepositoryPropertiesField;
import ru.jamsys.core.extension.property.repository.RepositoryPropertiesMap;
import ru.jamsys.core.flat.util.Util;

import java.util.concurrent.atomic.AtomicInteger;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PropertiesAgentTest {

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
    @Order(1)
    public void collection() {
        RepositoryPropertiesMap<String> propertiesRepositoryMap = new RepositoryPropertiesMap<>(String.class);
        PropertiesAgent propertiesAgent = App
                .get(ServiceProperty.class)
                .getFactory()
                .getPropertiesAgent(null, propertiesRepositoryMap, "run.args.IgnoreClassFinder", false);

        propertiesAgent.series("run\\.args\\.IgnoreClassFinder.*");

        Assertions.assertEquals("{test1=true, test2=false}", propertiesRepositoryMap.unitTestGetProperties().toString());
        Assertions.assertEquals("[run\\.args\\.IgnoreClassFinder.*]", propertiesAgent.getRepositoryPropertyListeners().toString());

        // Дребидень полная в случае с regexp
        Assertions.assertEquals("[run.args.IgnoreClassFinder.run\\.args\\.IgnoreClassFinder.*]", propertiesAgent.getServicePropertyListeners().toString());

        Assertions.assertEquals("{test1=true, test2=false}", propertiesAgent.getRepositoryProperties().unitTestGetProperties().toString());

        App.get(ServiceProperty.class).setProperty("run.args.IgnoreClassFinder.test1", "false");

        Assertions.assertEquals("{test1=false, test2=false}", propertiesAgent.getRepositoryProperties().unitTestGetProperties().toString());

        Assertions.assertEquals("false", App.get(ServiceProperty.class).unitTestGetProp("run.args.IgnoreClassFinder.test2"));

        propertiesAgent.setPropertyRepository("test2", "true");

        Assertions.assertEquals("{test1=false, test2=true}", propertiesAgent.getRepositoryProperties().unitTestGetProperties().toString());

        Assertions.assertEquals("true", App.get(ServiceProperty.class).unitTestGetProp("run.args.IgnoreClassFinder.test2"));

    }

    @Test
    @Order(2)
    public void onUpdate() {
        RepositoryPropertiesMap<String> propertiesRepositoryMap = new RepositoryPropertiesMap<>(String.class);
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
        RepositoryPropertiesMap<String> propertiesRepositoryMap = new RepositoryPropertiesMap<>(String.class);
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
                    Util.logConsole(getClass(), aBoolean.toString());
                    x.incrementAndGet();
                });
        Assertions.assertEquals(1, x.get());

        propertiesAgent.setPropertyRepository("run.args.IgnoreClassFinder.test1", "false");

        Assertions.assertEquals(2, x.get());

        Assertions.assertEquals("{run.args.IgnoreClassFinder.test1=false}", propertiesAgent.getRepositoryProperties().unitTestGetProperties().toString());

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

    @Test
    public void extend() {
        RepositoryPropertiesMap<String> propertiesRepositoryMap = new RepositoryPropertiesMap<>(String.class);
        AtomicInteger x = new AtomicInteger(0);
        PropertiesAgent propertiesAgent = App
                .get(ServiceProperty.class)
                .getFactory()
                .getPropertiesAgent(
                        mapAlias -> x.addAndGet(mapAlias.size()),
                        propertiesRepositoryMap,
                        "extend",
                        false
                );

        propertiesAgent.series("extend.*");

        Assertions.assertEquals("{}", propertiesRepositoryMap.unitTestGetProperties().toString());

        App.get(ServiceProperty.class).setProperty("extend.hello", "world");

        Assertions.assertEquals("{hello=world}", propertiesRepositoryMap.unitTestGetProperties().toString());
        Assertions.assertEquals(1, x.get());

        App.get(ServiceProperty.class).setProperty("extend.hello", "kitty");
        Assertions.assertEquals("{hello=kitty}", propertiesRepositoryMap.unitTestGetProperties().toString());
        Assertions.assertEquals(2, x.get());

        App.get(ServiceProperty.class).setProperty("extend.secondary", "xxkaa");
        Assertions.assertEquals("{hello=kitty, secondary=xxkaa}", propertiesRepositoryMap.unitTestGetProperties().toString());
        Assertions.assertEquals(3, x.get());

        App.get(ServiceProperty.class).setProperty(
                new HashMapBuilder<String, String>()
                        .append("extend.hello", "x1")
                        .append("extend.secondary", "x2")
        );
        Assertions.assertEquals("{hello=x1, secondary=x2}", propertiesRepositoryMap.unitTestGetProperties().toString());
        Assertions.assertEquals(5, x.get());

        App.get(ServiceProperty.class).setProperty("extend.secondary", null);
        Assertions.assertEquals("{hello=x1, secondary=null}", propertiesRepositoryMap.unitTestGetProperties().toString());
        Assertions.assertEquals(6, x.get());

        // Попытка повторного зануления не должна приводить к выхову onUpdate
        App.get(ServiceProperty.class).setProperty("extend.secondary", null);
        Assertions.assertEquals(6, x.get());

    }

    @Getter
    public static class TypedProperty extends RepositoryPropertiesField {

        @PropertyName("run.args.IgnoreClassFinder.test1")
        private Boolean test;

    }

    @Test
    @Order(7)
    public void typedTest() {
        TypedProperty typedProperty = new TypedProperty();
        PropertiesAgent propertiesAgent = App
                .get(ServiceProperty.class)
                .getFactory()
                .getPropertiesAgent(
                        null,
                        typedProperty,
                        null,
                        true
                );
        Assertions.assertEquals(true, typedProperty.getTest());
        propertiesAgent.setPropertyRepository("run.args.IgnoreClassFinder.test1", "false");
        Assertions.assertEquals(false, typedProperty.getTest());
        propertiesAgent.setPropertyRepository("run.args.IgnoreClassFinder.test1", "true");
        Assertions.assertEquals(true, typedProperty.getTest());
    }


}