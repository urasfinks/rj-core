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
        App.get(ServiceProperty.class).computeIfAbsent("run.args.ServiceClassFinderIgnore.test1", "true", PropertySubscriptionTest.class.getName());
        App.get(ServiceProperty.class).computeIfAbsent("run.args.ServiceClassFinderIgnore.test2", "false", PropertySubscriptionTest.class.getName());
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @Test
    @Order(1)
    public void collection() {
        PropertyRepositoryMap<String> propertiesRepositoryMap = new PropertyRepositoryMap<>(String.class);
        PropertySubscriber propertySubscriber = new PropertySubscriber(App.get(ServiceProperty.class), null, propertiesRepositoryMap, "run.args.ServiceClassFinderIgnore").addSubscriptionRegexp("run\\.args\\.ServiceClassFinderIgnore.*");
        propertySubscriber.run();

        Assertions.assertEquals("{test1=true, test2=false}", propertiesRepositoryMap.getMapRepository().toString());
        //Assertions.assertEquals("[run\\.args\\.ServiceClassFinderIgnore.*]", propertySubscriber.getRepositoryPropertyListeners().toString());

        // Дребидень полная в случае с regexp
        //Assertions.assertEquals("[run.args.ServiceClassFinderIgnore.run\\.args\\.ServiceClassFinderIgnore.*]", propertySubscriber.getServicePropertyListeners().toString());

        Assertions.assertEquals("{test1=true, test2=false}", propertySubscriber.getPropertyRepository().getRepository().toString());

        App.get(ServiceProperty.class).computeIfAbsent("run.args.ServiceClassFinderIgnore.test1", null, PropertySubscriptionTest.class.getName()).set(false, PropertySubscriptionTest.class.getName());

        Assertions.assertEquals("false", App.get(ServiceProperty.class).computeIfAbsent("run.args.ServiceClassFinderIgnore.test1", null, PropertySubscriptionTest.class.getName()).get());

        Assertions.assertEquals("{test1=false, test2=false}", propertySubscriber.getPropertyRepository().getRepository().toString());

        Assertions.assertEquals("false", App.get(ServiceProperty.class).computeIfAbsent("run.args.ServiceClassFinderIgnore.test2", null, PropertySubscriptionTest.class.getName()).get());

        propertySubscriber.getPropertyRepository().setRepository("test2", "true");

        Assertions.assertEquals("{test1=false, test2=true}", propertySubscriber.getPropertyRepository().getRepository().toString());

        // setRepository("test2", "true") не влияет на изменения Property, все изменения только через Property сверху в низ
        Assertions.assertEquals("false", App.get(ServiceProperty.class).computeIfAbsent("run.args.ServiceClassFinderIgnore.test2", "", PropertySubscriptionTest.class.getName()).get());

    }

    @Test
    @Order(2)
    public void onUpdate() {
        // ВНИМАТЕЛЬНО тесты выполняются по очереди, по отдельности выполнять нельзя
        PropertyRepositoryMap<String> propertiesRepositoryMap = new PropertyRepositoryMap<>(String.class);
        AtomicInteger x = new AtomicInteger(0);
        PropertySubscriber propertySubscriber = new PropertySubscriber(App.get(ServiceProperty.class), (_, _, _) -> x.incrementAndGet(), propertiesRepositoryMap, "run.args.ServiceClassFinderIgnore").addSubscriptionRegexp("run\\.args\\.ServiceClassFinderIgnore.*");
        propertySubscriber.run();

        Assertions.assertEquals("{test1=false, test2=false}", propertySubscriber.getPropertyRepository().getRepository().toString());

        // Инициализация подписчика, не вызывает событий обновления, она проливает данные до репозитория
        // События наступают только после обновления данных Property
        Assertions.assertEquals(0, x.get());

        App.get(ServiceProperty.class).computeIfAbsent("run.args.ServiceClassFinderIgnore.test1", null, PropertySubscriptionTest.class.getName()).set(false, PropertySubscriptionTest.class.getName());

        // Так как значение не поменялось true -> true
        Assertions.assertEquals(0, x.get());

        App.get(ServiceProperty.class).computeIfAbsent("run.args.ServiceClassFinderIgnore.test1", null, PropertySubscriptionTest.class.getName()).set(true, PropertySubscriptionTest.class.getName());

        // Так как значение не поменялось true -> true
        Assertions.assertEquals(1, x.get());

        App.get(ServiceProperty.class).computeIfAbsent("run.args.ServiceClassFinderIgnore.test3", null, PropertySubscriptionTest.class.getName());

        Assertions.assertEquals("{test1=true, test2=false, test3=null}", propertySubscriber.getPropertyRepository().getRepository().toString());
        Assertions.assertEquals(2, x.get());

        App.get(ServiceProperty.class).computeIfAbsent("run.args.ServiceClassFinderIgnore.test3", null, PropertySubscriptionTest.class.getName()).set(true, PropertySubscriptionTest.class.getName());

        Assertions.assertEquals("{test1=true, test2=false, test3=true}", propertySubscriber.getPropertyRepository().getRepository().toString());
        Assertions.assertEquals(3, x.get());
    }

    @Getter
    public static class TypedProperty extends AnnotationPropertyExtractor {

        @SuppressWarnings("all")
        @PropertyName("run.args.ServiceClassFinderIgnore.test1")
        private Boolean test;

    }

    @Test
    @Order(7)
    public void typedTest() {
        // ВНИМАТЕЛЬНО тесты выполняются по очереди, по отдельности выполнять нельзя
        TypedProperty typedProperty = new TypedProperty();
        PropertySubscriber propertySubscriber = new PropertySubscriber(App.get(ServiceProperty.class), null, typedProperty, null);

        propertySubscriber.run();

        Assertions.assertEquals(true, typedProperty.getTest());
        App.get(ServiceProperty.class).computeIfAbsent("run.args.ServiceClassFinderIgnore.test1", "", PropertySubscriptionTest.class.getName()).set(false, PropertySubscriptionTest.class.getName());
        Assertions.assertEquals(false, typedProperty.getTest());
        App.get(ServiceProperty.class).computeIfAbsent("run.args.ServiceClassFinderIgnore.test1", "", PropertySubscriptionTest.class.getName()).set(true, PropertySubscriptionTest.class.getName());
        Assertions.assertEquals(true, typedProperty.getTest());
    }


}