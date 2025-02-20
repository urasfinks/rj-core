package ru.jamsys.core;

import lombok.Getter;
import org.junit.jupiter.api.*;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.annotation.PropertyName;
import ru.jamsys.core.extension.property.PropertySubscriber;
import ru.jamsys.core.extension.property.repository.AnnotationPropertyExtractor;
import ru.jamsys.core.extension.property.repository.PropertyRepositoryMap;

import java.util.concurrent.atomic.AtomicInteger;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PropertySubscriptionTest {

    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runCore();
        App.get(ServiceProperty.class).get("run.args.IgnoreClassFinder.test1", "true", PropertySubscriptionTest.class.getName());
        App.get(ServiceProperty.class).get("run.args.IgnoreClassFinder.test2", "false", PropertySubscriptionTest.class.getName());
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @Test
    @Order(1)
    public void collection() {
        PropertyRepositoryMap<String> propertiesRepositoryMap = new PropertyRepositoryMap<>(String.class);
        PropertySubscriber propertySubscriber = new PropertySubscriber(
                App.get(ServiceProperty.class),
                null,
                propertiesRepositoryMap,
                "run.args.IgnoreClassFinder"
        ).addSubscriptionPattern("run\\.args\\.IgnoreClassFinder.*");


        Assertions.assertEquals("{test1=true, test2=false}", propertiesRepositoryMap.getMapRepository().toString());
        //Assertions.assertEquals("[run\\.args\\.IgnoreClassFinder.*]", propertySubscriber.getRepositoryPropertyListeners().toString());

        // Дребидень полная в случае с regexp
        //Assertions.assertEquals("[run.args.IgnoreClassFinder.run\\.args\\.IgnoreClassFinder.*]", propertySubscriber.getServicePropertyListeners().toString());

        Assertions.assertEquals("{test1=true, test2=false}", propertySubscriber.getPropertyRepository().getRepository().toString());

        App.get(ServiceProperty.class)
                .get("run.args.IgnoreClassFinder.test1", "false", PropertySubscriptionTest.class.getName())
                .set(false, PropertySubscriptionTest.class.getName());

        Assertions.assertEquals("{test1=false, test2=false}", propertySubscriber.getPropertyRepository().getRepository().toString());

        Assertions.assertEquals("false", App.get(ServiceProperty.class)
                .get("run.args.IgnoreClassFinder.test2", "", PropertySubscriptionTest.class.getName())
                .get()
        );

        propertySubscriber.getPropertyRepository().setRepository("test2", "true");

        Assertions.assertEquals("{test1=false, test2=true}", propertySubscriber.getPropertyRepository().getRepository().toString());

        Assertions.assertEquals("true", App.get(ServiceProperty.class).get("run.args.IgnoreClassFinder.test2", "", PropertySubscriptionTest.class.getName()));

    }

    @Test
    @Order(2)
    public void onUpdate() {
        PropertyRepositoryMap<String> propertiesRepositoryMap = new PropertyRepositoryMap<>(String.class);
        AtomicInteger x = new AtomicInteger(0);
        PropertySubscriber propertySubscriber = new PropertySubscriber(
                App.get(ServiceProperty.class),
                (key, property) -> {
                    x.incrementAndGet();
                },
                propertiesRepositoryMap,
                "run.args.IgnoreClassFinder"
        ).addSubscriptionPattern("run\\.args\\.IgnoreClassFinder.*");

        Assertions.assertEquals(2, x.get());

        Assertions.assertEquals(3, x.get());

    }


    @Test
    @Order(6)
    public void simpleUpdate() {

    }

    @Test
    public void extendSeries() {

    }

    @Getter
    public static class TypedProperty extends AnnotationPropertyExtractor {

        @PropertyName("run.args.IgnoreClassFinder.test1")
        private Boolean test;

    }

    @Test
    @Order(7)
    public void typedTest() {
        TypedProperty typedProperty = new TypedProperty();
        PropertySubscriber propertySubscriber = new PropertySubscriber(
                App.get(ServiceProperty.class),
                null,
                typedProperty,
                null
        );

        propertySubscriber.run();

        Assertions.assertEquals(true, typedProperty.getTest());
        App.get(ServiceProperty.class)
                .get("run.args.IgnoreClassFinder.test1", "", PropertySubscriptionTest.class.getName())
                .set(false, PropertySubscriptionTest.class.getName());
        Assertions.assertEquals(false, typedProperty.getTest());
        App.get(ServiceProperty.class)
                .get("run.args.IgnoreClassFinder.test1", "", PropertySubscriptionTest.class.getName())
                .set(true, PropertySubscriptionTest.class.getName());
        Assertions.assertEquals(true, typedProperty.getTest());
    }


}