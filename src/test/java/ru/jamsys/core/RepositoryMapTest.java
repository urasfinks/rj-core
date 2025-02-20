package ru.jamsys.core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.annotation.PropertyName;
import ru.jamsys.core.extension.property.Property;
import ru.jamsys.core.extension.property.PropertySubscriber;
import ru.jamsys.core.extension.property.PropertyUpdater;
import ru.jamsys.core.extension.property.repository.AnnotationPropertyExtractor;
import ru.jamsys.core.flat.util.Util;

import java.util.Map;

// IO time: 5ms
// COMPUTE time: 5ms

class RepositoryMapTest {
    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runCore();
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    public static class XX extends AnnotationPropertyExtractor implements PropertyUpdater {

        int c = 0;

        @Override
        public void onPropertyUpdate(String key, Property property) {
            c++;
        }
    }

    @Test
    void test() {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        XX xx = new XX();
        PropertySubscriber propertySubscriber = new PropertySubscriber(
                serviceProperty,
                xx,
                xx,
                null
        );

        propertySubscriber
                .addSubscription( "run.args.security.path.storage")
                .addSubscription( "run.args.security.path.storage");


        Assertions.assertEquals(1, xx.c);

        Assertions.assertEquals(1, propertySubscriber.getSubscriptions().size());

        propertySubscriber.addSubscription("run.args.security.path.public.key");

        Assertions.assertEquals(2, propertySubscriber.getSubscriptions().size());

        Assertions.assertEquals(2, xx.c);

        serviceProperty
                .get("run.args.security.path.storage", "xx", RepositoryMapTest.class.getName())
                .set("xx", RepositoryMapTest.class.getName());
        Assertions.assertEquals(3, xx.c);

        // Дубликат значения не должен вызывать onPropUpdate
        serviceProperty
                .get("run.args.security.path.storage", "xx", RepositoryMapTest.class.getName())
                .set("xx", RepositoryMapTest.class.getName());
        Assertions.assertEquals(3, xx.c);

        propertySubscriber.shutdown();
        //Assertions.assertEquals(0, propertySubscriber.getListSubscriber().size());

        // После отписки мы не должны получать уведомления об изменениях

        serviceProperty
                .get("run.args.security.path.storage", "x2", RepositoryMapTest.class.getName())
                .set("x2", RepositoryMapTest.class.getName());
        Assertions.assertEquals(3, xx.c);

        // Обратно подписываемся
        propertySubscriber.addSubscription("run.args.security.path.storage");
        // Так как автоматом получим значение при подписке
        Assertions.assertEquals(4, xx.c);

        propertySubscriber.addSubscription("run.args.security.path.public.key");
        // Так как автоматом получим значение при подписке
        Assertions.assertEquals(5, xx.c);

        serviceProperty
                .get("run.args.security.path.public.key", "", RepositoryMapTest.class.getName())
                .set("x3", RepositoryMapTest.class.getName());
        Assertions.assertEquals(6, xx.c);

        propertySubscriber.removeByRepositoryKey("run.args.security.path.public.key");

        serviceProperty
                .get("run.args.security.path.public.key", "", RepositoryMapTest.class.getName())
                .set("x4", RepositoryMapTest.class.getName());
        Assertions.assertEquals(6, xx.c);

        // Проверяем что другие подписки работают
        serviceProperty
                .get("run.args.security.path.storage", "", RepositoryMapTest.class.getName())
                .set("x5", RepositoryMapTest.class.getName());
        Assertions.assertEquals(7, xx.c);


        Assertions.assertEquals(7, xx.c);

    }


    static class x2 extends AnnotationPropertyExtractor implements PropertyUpdater {

        @SuppressWarnings("all")
        @PropertyName("security.path.storage")
        public String storage = "wef";

        @SuppressWarnings("all")
        @PropertyName("security.path.public.key")
        public String publicKey = "ppbb";

        @Override
        public void onPropertyUpdate(String key, Property property) {
            Util.logConsole(getClass(), key);
        }
    }

    @Test
    void testEnum() {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        x2 x2 = new x2();

        Map<String, String> mapPropValue = x2.getRepository();
        System.out.println(mapPropValue);

        PropertySubscriber subscribe = new PropertySubscriber(serviceProperty, x2, x2, "run.args");

        Assertions.assertEquals(2, subscribe.getSubscriptions().size());

        Assertions.assertEquals("security/security.jks", x2.storage);

        serviceProperty
                .get("run.args.security.path.storage", "x3", RepositoryMapTest.class.getName())
                .set("x3", RepositoryMapTest.class.getName());
        Assertions.assertEquals("x3", x2.storage);

    }

}