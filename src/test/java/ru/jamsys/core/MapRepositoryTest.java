package ru.jamsys.core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.property.PropertiesRepository;
import ru.jamsys.core.extension.annotation.PropertyName;
import ru.jamsys.core.extension.property.PropertyUpdateDelegate;
import ru.jamsys.core.extension.property.PropertiesNsAgent;

import java.util.Map;
import java.util.Set;

// IO time: 5ms
// COMPUTE time: 5ms

class MapRepositoryTest {
    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runCore();
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    public static class XX extends PropertiesRepository implements PropertyUpdateDelegate {

        int c = 0;

        @Override
        public void onPropertyUpdate(Set<String> updatedPropAlias) {
            c += updatedPropAlias.size();
            System.out.println(updatedPropAlias);
        }
    }

    @Test
    void test() {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        XX xx = new XX();
        PropertiesNsAgent subscribe = serviceProperty
                .getFactory()
                .getNsAgent(xx, xx)
                .add("run.args.security.path.storage", null, true)
                .add("run.args.security.path.storage", null, true);

        Assertions.assertEquals(1, subscribe.getMapListener().size());

        subscribe.add("run.args.security.path.public.key", null, true);

        Assertions.assertEquals(2, subscribe.getMapListener().size());

        Assertions.assertEquals(2, xx.c);

        serviceProperty.setProperty("run.args.security.path.storage", "xx");
        Assertions.assertEquals(3, xx.c);

        // Дубликат значения не должен вызывать onPropUpdate
        serviceProperty.setProperty("run.args.security.path.storage", "xx");
        Assertions.assertEquals(3, xx.c);

        subscribe.shutdown();
        Assertions.assertEquals(0, subscribe.getCountListener());

        // После отписки мы не должны получать уведомления об изменениях
        serviceProperty.setProperty("run.args.security.path.storage", "x2");
        Assertions.assertEquals(3, xx.c);

        // Обратно подписываемся
        subscribe.add("run.args.security.path.storage", null, true);
        // Так как автоматом получим значение при подписке
        Assertions.assertEquals(4, xx.c);

        subscribe.add("run.args.security.path.public.key", null, true);
        // Так как автоматом получим значение при подписке
        Assertions.assertEquals(5, xx.c);

        serviceProperty.setProperty("run.args.security.path.public.key", "x3");
        Assertions.assertEquals(6, xx.c);

        subscribe.remove("run.args.security.path.public.key");
        serviceProperty.setProperty("run.args.security.path.public.key", "x4");
        Assertions.assertEquals(6, xx.c);

        // Проверяем что другие подписки работают
        serviceProperty.setProperty("run.args.security.path.storage", "x5");
        Assertions.assertEquals(7, xx.c);

        //Мульти обновление без изменений значений
        serviceProperty.setProperty(new HashMapBuilder<String, String>()
                .append("run.args.security.path.public.key", "x4")
                .append("run.args.security.path.storage", "x5")
        );
        Assertions.assertEquals(7, xx.c);


        //Мульти обновление c неподписанным
        serviceProperty.setProperty(new HashMapBuilder<String, String>()
                .append("run.args.security.path.public.key", "new") // Подписки нет, но значение изменено
                .append("run.args.security.path.storage", "x5") // Подписка есть но значение старое
        );
        Assertions.assertEquals(7, xx.c);

        subscribe.add("run.args.security.path.public.key", null,true);
        Assertions.assertEquals(8, xx.c);

        //Мульти обновление штатное
        serviceProperty.setProperty(new HashMapBuilder<String, String>()
                .append("run.args.security.path.public.key", "new2")
                .append("run.args.security.path.storage", "x5")
        );
        Assertions.assertEquals(10, xx.c);

        //Мульти обновление штатное
        serviceProperty.setProperty(new HashMapBuilder<String, String>()
                .append("run.args.security.path.public.key", "new2")
                .append("run.args.security.path.storage", "x6")
        );
        Assertions.assertEquals(12, xx.c);

    }


    static class x2 extends PropertiesRepository implements PropertyUpdateDelegate {

        @SuppressWarnings("all")
        @PropertyName("security.path.storage")
        public String storage = "wef";

        @SuppressWarnings("all")
        @PropertyName("security.path.public.key")
        public String publicKey = "ppbb";

        @Override
        public void onPropertyUpdate(Set<String> updatedPropAlias) {
            System.out.println(updatedPropAlias);
        }
    }

    @Test
    void testEnum() {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        x2 x2 = new x2();

        Map<String, String> mapPropValue = x2.getMapPropValue();
        System.out.println(mapPropValue);

        PropertiesNsAgent subscribe = serviceProperty.getFactory().getNsAgent(x2, x2, "run.args");

        Assertions.assertEquals(2, subscribe.getMapListener().size());

        Assertions.assertEquals("security/security.jks", x2.storage);

        serviceProperty.setProperty("run.args.security.path.storage", "x3");
        Assertions.assertEquals("x3", x2.storage);

    }

}