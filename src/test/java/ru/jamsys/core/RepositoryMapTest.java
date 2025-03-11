package ru.jamsys.core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.annotation.PropertyName;
import ru.jamsys.core.extension.property.Property;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.PropertyListener;
import ru.jamsys.core.extension.property.repository.AnnotationPropertyExtractor;
import ru.jamsys.core.extension.property.repository.PropertyRepositoryMap;
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

    public static class XX extends PropertyRepositoryMap<String> implements PropertyListener {

        int c = 0;

        public XX() {
            super(String.class);
        }

        @Override
        public void onPropertyUpdate(String key, String oldValue, Property property) {
            c++;
        }
    }

    @Test
    void test() {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        XX xx = new XX();
        PropertyDispatcher propertyDispatcher = new PropertyDispatcher(
                serviceProperty,
                xx,
                xx,
                null
        );
        propertyDispatcher.run();

        propertyDispatcher
                .addSubscription("run.args.security.path.storage", null)
                .addSubscription("run.args.security.path.storage", null);

        propertyDispatcher.reload();

        Assertions.assertEquals(0, xx.c);

        Assertions.assertEquals(1, propertyDispatcher.getSubscriptions().size());

        propertyDispatcher.addSubscription("run.args.security.path.public.key", null);
        propertyDispatcher.reload();

        Assertions.assertEquals(2, propertyDispatcher.getSubscriptions().size());
        Util.logConsoleJson(getClass(), propertyDispatcher);
        Assertions.assertEquals(0, xx.c);

        serviceProperty.set("run.args.security.path.storage", "xx");
        Assertions.assertEquals(2, xx.c);

        // Дубликат значения не должен вызывать onPropUpdate
        serviceProperty.set("run.args.security.path.storage", "xx");
        Assertions.assertEquals(2, xx.c);

        propertyDispatcher.shutdown();
        //Assertions.assertEquals(0, propertySubscriber.getListSubscriber().size());

        // После отписки мы не должны получать уведомления об изменениях

        serviceProperty.set("run.args.security.path.storage", "x2");
        Assertions.assertEquals(3, xx.c);

        // Обратно подписываемся
        propertyDispatcher.addSubscription("run.args.security.path.storage", null);
        Assertions.assertEquals(3, xx.c);

        propertyDispatcher.addSubscription("run.args.security.path.public.key", null);
        Assertions.assertEquals(3, xx.c);

        serviceProperty.set("run.args.security.path.public.key", "x3");
        Assertions.assertEquals(3, xx.c);

        propertyDispatcher.removeSubscriptionByRepositoryKey("run.args.security.path.public.key");

        serviceProperty.set("run.args.security.path.public.key", "x4");
        Assertions.assertEquals(3, xx.c);

        // Проверяем что другие подписки работают
        serviceProperty.set("run.args.security.path.storage", "x5");
        Assertions.assertEquals(4, xx.c);

    }


    static class x2 extends AnnotationPropertyExtractor implements PropertyListener {

        @SuppressWarnings("all")
        @PropertyName("security.path.storage")
        public String storage = "wef";

        @SuppressWarnings("all")
        @PropertyName("security.path.public.key")
        public String publicKey = "ppbb";

        @Override
        public void onPropertyUpdate(String key, String oldValue, Property property) {
            Util.logConsole(getClass(), key);
        }
    }

    @Test
    void testEnum() {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        x2 x2 = new x2();

        Map<String, String> mapPropValue = x2.getRepository();
        System.out.println(mapPropValue);

        PropertyDispatcher subscribe = new PropertyDispatcher(serviceProperty, x2, x2, "run.args");
        subscribe.run();

        Assertions.assertEquals(2, subscribe.getSubscriptions().size());

        Assertions.assertEquals("security/security.jks", x2.storage);

        serviceProperty.set("run.args.security.path.storage", "x3");
        Assertions.assertEquals("x3", x2.storage);

    }

}