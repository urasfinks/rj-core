package ru.jamsys.core;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import org.junit.jupiter.api.*;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.annotation.PropertyKey;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.repository.RepositoryPropertyAnnotationField;
import ru.jamsys.core.extension.property.repository.RepositoryProperty;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilLog;

import java.util.concurrent.atomic.AtomicInteger;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PropertySubscriptionTest {

    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runCore();
        App.get(ServiceProperty.class).computeIfAbsent("run.args.ServiceClassFinderIgnore.test1", "true");
        App.get(ServiceProperty.class).computeIfAbsent("run.args.ServiceClassFinderIgnore.test2", "false");
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @Test
    @Order(1)
    public void collection() {
        RepositoryProperty<String> propertyRepositoryList = new RepositoryProperty<>(String.class);
        PropertyDispatcher<String> propertyDispatcher = new PropertyDispatcher<>(
                App.get(ServiceProperty.class),
                null,
                propertyRepositoryList,
                "run.args.ServiceClassFinderIgnore"
        )
                .addSubscriptionRegexp("run\\.args\\.ServiceClassFinderIgnore.*");

        propertyDispatcher.run();
        UtilLog.printInfo(propertyDispatcher);

        Assertions.assertEquals("""
                [
                  {
                    "propertyKey" : "run.args.ServiceClassFinderIgnore.test1",
                    "repositoryPropertyKey" : "test1",
                    "fieldNameConstants" : null,
                    "cls" : "java.lang.String",
                    "description" : null,
                    "notNull" : false,
                    "dynamic" : true,
                    "value" : "true"
                  },
                  {
                    "propertyKey" : "run.args.ServiceClassFinderIgnore.test2",
                    "repositoryPropertyKey" : "test2",
                    "fieldNameConstants" : null,
                    "cls" : "java.lang.String",
                    "description" : null,
                    "notNull" : false,
                    "dynamic" : true,
                    "value" : "false"
                  }
                ]""", UtilJson.toStringPretty(propertyRepositoryList.getListPropertyEnvelopeRepository(), "--"));
        //Assertions.assertEquals("[run\\.args\\.ServiceClassFinderIgnore.*]", propertySubscriber.getRepositoryPropertyListeners().toString());

        // Дребидень полная в случае с regexp
        //Assertions.assertEquals("[run.args.ServiceClassFinderIgnore.run\\.args\\.ServiceClassFinderIgnore.*]", propertySubscriber.getServicePropertyListeners().toString());

        Assertions.assertEquals("""
                [
                  {
                    "propertyKey" : "run.args.ServiceClassFinderIgnore.test1",
                    "repositoryPropertyKey" : "test1",
                    "fieldNameConstants" : null,
                    "cls" : "java.lang.String",
                    "description" : null,
                    "notNull" : false,
                    "dynamic" : true,
                    "value" : "true"
                  },
                  {
                    "propertyKey" : "run.args.ServiceClassFinderIgnore.test2",
                    "repositoryPropertyKey" : "test2",
                    "fieldNameConstants" : null,
                    "cls" : "java.lang.String",
                    "description" : null,
                    "notNull" : false,
                    "dynamic" : true,
                    "value" : "false"
                  }
                ]""", UtilJson.toStringPretty(propertyRepositoryList.getListPropertyEnvelopeRepository(), "--"));

        App.get(ServiceProperty.class).computeIfAbsent("run.args.ServiceClassFinderIgnore.test1", null).set(false);

        Assertions.assertEquals("false", App.get(ServiceProperty.class).computeIfAbsent("run.args.ServiceClassFinderIgnore.test1", null).get());

        Assertions.assertEquals("""
                [
                  {
                    "propertyKey" : "run.args.ServiceClassFinderIgnore.test1",
                    "repositoryPropertyKey" : "test1",
                    "fieldNameConstants" : null,
                    "cls" : "java.lang.String",
                    "description" : null,
                    "notNull" : false,
                    "dynamic" : true,
                    "value" : "false"
                  },
                  {
                    "propertyKey" : "run.args.ServiceClassFinderIgnore.test2",
                    "repositoryPropertyKey" : "test2",
                    "fieldNameConstants" : null,
                    "cls" : "java.lang.String",
                    "description" : null,
                    "notNull" : false,
                    "dynamic" : true,
                    "value" : "false"
                  }
                ]""", UtilJson.toStringPretty(propertyRepositoryList.getListPropertyEnvelopeRepository(), "--"));

        Assertions.assertEquals("false", App.get(ServiceProperty.class).computeIfAbsent("run.args.ServiceClassFinderIgnore.test2", null).get());

    }

    @Test
    @Order(2)
    public void onUpdate() {
        // ВНИМАТЕЛЬНО тесты выполняются по очереди, по отдельности выполнять нельзя
        RepositoryProperty<String> propertiesRepositoryMap = new RepositoryProperty<>(String.class);
        AtomicInteger x = new AtomicInteger(0);
        PropertyDispatcher<String> propertyDispatcher = new PropertyDispatcher<>(
                App.get(ServiceProperty.class),
                (_, _, _) -> x.incrementAndGet(),
                propertiesRepositoryMap,
                "run.args.ServiceClassFinderIgnore"
        )
                .addSubscriptionRegexp("run\\.args\\.ServiceClassFinderIgnore.*");
        propertyDispatcher.run();

        Assertions.assertEquals("""
                {
                  "cls" : "java.lang.String",
                  "listPropertyEnvelopeRepository" : [
                    {
                      "propertyKey" : "run.args.ServiceClassFinderIgnore.test1",
                      "repositoryPropertyKey" : "test1",
                      "fieldNameConstants" : null,
                      "cls" : "java.lang.String",
                      "description" : null,
                      "notNull" : false,
                      "dynamic" : true,
                      "value" : "false"
                    },
                    {
                      "propertyKey" : "run.args.ServiceClassFinderIgnore.test2",
                      "repositoryPropertyKey" : "test2",
                      "fieldNameConstants" : null,
                      "cls" : "java.lang.String",
                      "description" : null,
                      "notNull" : false,
                      "dynamic" : true,
                      "value" : "false"
                    }
                  ],
                  "init" : true
                }""", UtilJson.toStringPretty(propertyDispatcher.getRepositoryProperty(), "--"));

        // Инициализация подписчика, не вызывает событий обновления, она проливает данные до репозитория
        // События наступают только после обновления данных Property
        Assertions.assertEquals(0, x.get());

        App.get(ServiceProperty.class).computeIfAbsent("run.args.ServiceClassFinderIgnore.test1", null).set(false);

        // Так как значение не поменялось true -> true
        Assertions.assertEquals(0, x.get());

        App.get(ServiceProperty.class).computeIfAbsent("run.args.ServiceClassFinderIgnore.test1", null).set(true);

        // Так как значение не поменялось true -> true
        Assertions.assertEquals(1, x.get());

        App.get(ServiceProperty.class).computeIfAbsent("run.args.ServiceClassFinderIgnore.test3", null);

        Assertions.assertEquals("""
                {
                  "cls" : "java.lang.String",
                  "listPropertyEnvelopeRepository" : [
                    {
                      "propertyKey" : "run.args.ServiceClassFinderIgnore.test1",
                      "repositoryPropertyKey" : "test1",
                      "fieldNameConstants" : null,
                      "cls" : "java.lang.String",
                      "description" : null,
                      "notNull" : false,
                      "dynamic" : true,
                      "value" : "true"
                    },
                    {
                      "propertyKey" : "run.args.ServiceClassFinderIgnore.test2",
                      "repositoryPropertyKey" : "test2",
                      "fieldNameConstants" : null,
                      "cls" : "java.lang.String",
                      "description" : null,
                      "notNull" : false,
                      "dynamic" : true,
                      "value" : "false"
                    },
                    {
                      "propertyKey" : "run.args.ServiceClassFinderIgnore.test3",
                      "repositoryPropertyKey" : "test3",
                      "fieldNameConstants" : null,
                      "cls" : "java.lang.String",
                      "description" : null,
                      "notNull" : false,
                      "dynamic" : true,
                      "value" : null
                    }
                  ],
                  "init" : true
                }""", UtilJson.toStringPretty(propertyDispatcher.getRepositoryProperty(), "--"));
        Assertions.assertEquals(2, x.get());

        App.get(ServiceProperty.class).computeIfAbsent("run.args.ServiceClassFinderIgnore.test3", null).set(true);

        Assertions.assertEquals("""
                {
                  "cls" : "java.lang.String",
                  "listPropertyEnvelopeRepository" : [
                    {
                      "propertyKey" : "run.args.ServiceClassFinderIgnore.test1",
                      "repositoryPropertyKey" : "test1",
                      "fieldNameConstants" : null,
                      "cls" : "java.lang.String",
                      "description" : null,
                      "notNull" : false,
                      "dynamic" : true,
                      "value" : "true"
                    },
                    {
                      "propertyKey" : "run.args.ServiceClassFinderIgnore.test2",
                      "repositoryPropertyKey" : "test2",
                      "fieldNameConstants" : null,
                      "cls" : "java.lang.String",
                      "description" : null,
                      "notNull" : false,
                      "dynamic" : true,
                      "value" : "false"
                    },
                    {
                      "propertyKey" : "run.args.ServiceClassFinderIgnore.test3",
                      "repositoryPropertyKey" : "test3",
                      "fieldNameConstants" : null,
                      "cls" : "java.lang.String",
                      "description" : null,
                      "notNull" : false,
                      "dynamic" : true,
                      "value" : "true"
                    }
                  ],
                  "init" : true
                }""", UtilJson.toStringPretty(propertyDispatcher.getRepositoryProperty(), "--"));
        Assertions.assertEquals(3, x.get());
    }

    @FieldNameConstants
    @Getter
    public static class TypedProperty extends RepositoryPropertyAnnotationField<String> {

        @SuppressWarnings("all")
        @PropertyKey("run.args.ServiceClassFinderIgnore.test1")
        private Boolean test;

    }

    @Test
    @Order(7)
    public void typedTest() {
        // ВНИМАТЕЛЬНО тесты выполняются по очереди, по отдельности выполнять нельзя
        TypedProperty typedProperty = new TypedProperty();
        PropertyDispatcher<String> propertyDispatcher = new PropertyDispatcher<>(
                App.get(ServiceProperty.class),
                null,
                typedProperty,
                null
        );

        propertyDispatcher.run();

        Assertions.assertEquals(true, typedProperty.getTest());
        App.get(ServiceProperty.class).computeIfAbsent("run.args.ServiceClassFinderIgnore.test1", "").set(false);
        Assertions.assertEquals(false, typedProperty.getTest());
        App.get(ServiceProperty.class).computeIfAbsent("run.args.ServiceClassFinderIgnore.test1", "").set(true);
        Assertions.assertEquals(true, typedProperty.getTest());
    }


}