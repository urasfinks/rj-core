package ru.jamsys.core;

import lombok.experimental.FieldNameConstants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.annotation.PropertyKey;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.PropertyEnvelope;
import ru.jamsys.core.extension.property.PropertyListener;
import ru.jamsys.core.extension.property.repository.RepositoryProperty;
import ru.jamsys.core.extension.property.repository.RepositoryPropertyAnnotationField;
import ru.jamsys.core.flat.util.UtilLog;

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

    public static class XX extends RepositoryProperty<String> implements PropertyListener {

        int c = 0;

        public XX() {
            super(String.class);
        }

        @Override
        public void onPropertyUpdate(String key, String oldValue, String newValue) {
            c++;
        }
    }

    @Test
    void eq() {
        XX xx = new XX();
        PropertyEnvelope<String> p1 = new PropertyEnvelope<>(
                xx,
                null,
                String.class,
                null,
                "run.args.security.path.storage",
                null,
                null,
                null,
                false,
                true
        );
        PropertyEnvelope<String> p2 = new PropertyEnvelope<>(
                xx,
                null,
                String.class,
                null,
                "run.args.security.path.storage",
                null,
                null,
                null,
                false,
                true
        );
        Assertions.assertEquals(p1, p2);
    }

    @Test
    void test() {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        XX xx = new XX();
        PropertyDispatcher<String> propertyDispatcher = new PropertyDispatcher<>(
                xx,
                xx,
                null
        );
        propertyDispatcher.run();

        xx.append("run.args.security.path.storage", propertyDispatcher);
        xx.append("run.args.security.path.storage", propertyDispatcher);
        propertyDispatcher.reload();

        Assertions.assertEquals(0, xx.c);

        Assertions.assertEquals(1, propertyDispatcher.getSubscriptions().size());

        xx.append("run.args.security.path.public.key", propertyDispatcher);
        propertyDispatcher.reload();

        Assertions.assertEquals(2, propertyDispatcher.getSubscriptions().size());
        //UtilLog.printInfo(getClass(), propertyDispatcher);
        Assertions.assertEquals(0, xx.c);

        serviceProperty.set("run.args.security.path.storage", "xx");
        // Обновили 1 значение, должно прийти 1 обновление
        Assertions.assertEquals(1, xx.c);

        // Дубликат значения не должен вызывать onPropUpdate
        serviceProperty.set("run.args.security.path.storage", "xx");
        Assertions.assertEquals(1, xx.c);



        propertyDispatcher.shutdown();
        //Assertions.assertEquals(0, propertySubscriber.getListSubscriber().size());

        // После отписки мы не должны получать уведомления об изменениях

        serviceProperty.set("run.args.security.path.storage", "x2");
        Assertions.assertEquals(1, xx.c);

        // Обратно подписываемся
        propertyDispatcher.run();
        // Но после обратной подписки мы должны получить уведомление, что произошли изменения так как
        // В репе лежит xx, а в момент простоя значение поменялось на x2
        Assertions.assertEquals(2, xx.c);

        xx.append("run.args.security.path.public.key", propertyDispatcher);
        Assertions.assertEquals(2, xx.c);

        serviceProperty.set("run.args.security.path.public.key", "x3");
        Assertions.assertEquals(3, xx.c);

        propertyDispatcher.removeSubscriptionByRepositoryPropertyKey("run.args.security.path.public.key");

        serviceProperty.set("run.args.security.path.public.key", "x4");
        Assertions.assertEquals(3, xx.c);

        // Проверяем что другие подписки работают
        serviceProperty.set("run.args.security.path.storage", "x5");
        Assertions.assertEquals(4, xx.c);

    }

    @FieldNameConstants
    static class x2 extends RepositoryPropertyAnnotationField<String> implements PropertyListener {

        @SuppressWarnings("all")
        @PropertyKey("security.path.storage")
        public String storage = "wef";

        @SuppressWarnings("all")
        @PropertyKey("security.path.public.key")
        public String publicKey = "ppbb";

        @Override
        public void onPropertyUpdate(String key, String oldValue, String newValue) {
            UtilLog.printInfo(key);
        }
    }

    @Test
    void testEnum() {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        x2 x2 = new x2();


        PropertyDispatcher<String> subscribe = new PropertyDispatcher<>(x2, x2, "run.args");
        subscribe.run();

        Assertions.assertEquals(2, subscribe.getSubscriptions().size());

        Assertions.assertEquals("security/security.jks", x2.storage);

        serviceProperty.set("run.args.security.path.storage", "x3");
        Assertions.assertEquals("x3", x2.storage);

    }

}