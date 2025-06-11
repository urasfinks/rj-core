package ru.jamsys.core.extension.property.repository;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.junit.jupiter.api.*;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.annotation.PropertyKey;
import ru.jamsys.core.extension.annotation.PropertyNotNull;
import ru.jamsys.core.extension.annotation.PropertyValueRegexp;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;

class RepositoryPropertyBuilderTest {

    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runCore();
    }

    @BeforeEach
    void beforeEach() {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        serviceProperty.set("xx.t", "hello");
        serviceProperty.set("xx.t2", "world");
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @SuppressWarnings("all")
    @Getter
    @Setter
    @FieldNameConstants
    public static class AnyRepo extends RepositoryPropertyAnnotationField<Object> {

        @PropertyNotNull
        @PropertyKey("t")
        private String any = "xx";

        @PropertyNotNull
        @PropertyKey("t2")
        private String any2;
    }

    @Test
    void test() {
        AnyRepo xx = new RepositoryPropertyBuilder<>(new AnyRepo(), "xx")
                .applyServicePropertyOnlyNull()
                .build();
        Assertions.assertEquals("xx", xx.getAny());
        Assertions.assertEquals("world", xx.getAny2());
    }

    @Test
    void test2() {
        AnyRepo xx = new RepositoryPropertyBuilder<>(new AnyRepo(), "xx")
                .applyServiceProperty()
                .build();
        Assertions.assertEquals("hello", xx.getAny());
        Assertions.assertEquals("world", xx.getAny2());
    }

    @Test
    void test3() {
        Assertions.assertThrows(ForwardException.class, () -> {
            AnyRepo xx = new RepositoryPropertyBuilder<>(new AnyRepo(), "xx")
                    .apply(AnyRepo.Fields.any, "yy")
                    .build();
            Assertions.assertEquals("yy", xx.getAny());
        });
    }

    @Test
    void test4() {
        AnyRepo xx = new RepositoryPropertyBuilder<>(new AnyRepo(), "xx")
                .apply(AnyRepo.Fields.any, "yy")
                .apply(AnyRepo.Fields.any2, "yy2")
                .build();
        Assertions.assertEquals("yy", xx.getAny());
        Assertions.assertEquals("yy2", xx.getAny2());
    }

    @Test
    void test4_2() {
        AnyRepo xx = new RepositoryPropertyBuilder<>(new AnyRepo(), "xx")
                .applyMap(new HashMapBuilder<String, String>()
                        .append("any", "yy")
                        .append("any2", "yy2")
                )
                .build();
        Assertions.assertEquals("yy", xx.getAny());
        Assertions.assertEquals("yy2", xx.getAny2());
    }

    @SuppressWarnings("all")
    @Getter
    @Setter
    @FieldNameConstants
    public static class AnyRepo2 extends RepositoryPropertyAnnotationField<Object> {

        @PropertyNotNull
        @PropertyValueRegexp("^(GET|POST|PUT|DELETE|HEAD|OPTIONS|PATCH|TRACE|CONNECT)$")
        @PropertyKey("t")
        private String any = "xx";

    }

    @Test
    void test5() {
        Assertions.assertThrows(ForwardException.class, () -> new RepositoryPropertyBuilder<>(new AnyRepo2(), "xx")
                .build()
        );
    }

    @Test
    void test6() {
        AnyRepo2 xx = new RepositoryPropertyBuilder<>(new AnyRepo2(), "xx")
                .apply(AnyRepo2.Fields.any, "GET")
                .build();
        Assertions.assertEquals("GET", xx.getAny());
        // Проверяем, что нет влияния если в ServiceProperty поменяем значения
        App.get(ServiceProperty.class).set("xx.t", "hello");
        Assertions.assertEquals("GET", xx.getAny());
    }

}