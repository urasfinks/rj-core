package ru.jamsys.core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.annotation.PropertyName;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.property.PropertiesAgent;
import ru.jamsys.core.extension.property.PropertyUpdateDelegate;
import ru.jamsys.core.extension.property.item.PropertySubscriber;
import ru.jamsys.core.extension.property.repository.RepositoryPropertiesField;
import ru.jamsys.core.flat.util.Util;

import java.util.ArrayList;
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

    public static class XX extends RepositoryPropertiesField implements PropertyUpdateDelegate {

        int c = 0;

        @Override
        public void onPropertyUpdate(Map<String, String> mapAlias) {
            c += mapAlias.size();
            System.out.println(mapAlias);
        }
    }

    @Test
    void test() {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        XX xx = new XX();
        PropertiesAgent propertiesAgent = serviceProperty
                .getFactory()
                .getPropertiesAgent(xx, xx, null, true)
                .add(String.class, "run.args.security.path.storage", null, true, null)
                .add(String.class, "run.args.security.path.storage", null, true, null);

        Assertions.assertEquals(1, xx.c);

        Assertions.assertEquals(1, propertiesAgent.getMapSubscription().size());

        propertiesAgent.add(String.class,"run.args.security.path.public.key", null, true, null);

        Assertions.assertEquals(2, propertiesAgent.getMapSubscription().size());

        Assertions.assertEquals(2, xx.c);

        serviceProperty.setProperty("run.args.security.path.storage", "xx");
        Assertions.assertEquals(3, xx.c);

        // Дубликат значения не должен вызывать onPropUpdate
        serviceProperty.setProperty("run.args.security.path.storage", "xx");
        Assertions.assertEquals(3, xx.c);

        propertiesAgent.shutdown();
        Assertions.assertEquals(0, propertiesAgent.getListSubscriber().size());

        // После отписки мы не должны получать уведомления об изменениях
        serviceProperty.setProperty("run.args.security.path.storage", "x2");
        Assertions.assertEquals(3, xx.c);

        // Обратно подписываемся
        propertiesAgent.add(String.class,"run.args.security.path.storage", null, true, null);
        // Так как автоматом получим значение при подписке
        Assertions.assertEquals(4, xx.c);

        propertiesAgent.add(String.class,"run.args.security.path.public.key", null, true, null);
        // Так как автоматом получим значение при подписке
        Assertions.assertEquals(5, xx.c);

        serviceProperty.setProperty("run.args.security.path.public.key", "x3");
        Assertions.assertEquals(6, xx.c);

        propertiesAgent.removeByRepositoryKey("run.args.security.path.public.key");
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

        Assertions.assertEquals("x4", serviceProperty.unitTestGetProp("run.args.security.path.public.key"));
        Assertions.assertEquals("x5", serviceProperty.unitTestGetProp("run.args.security.path.storage"));

        //Мульти обновление c неподписанным
        serviceProperty.setProperty(new HashMapBuilder<String, String>()
                .append("run.args.security.path.public.key", "new") // Подписки нет, но значение изменено
                .append("run.args.security.path.storage", "x5") // Подписка есть но значение старое
        );

        Assertions.assertEquals("new", serviceProperty.unitTestGetProp("run.args.security.path.public.key"));
        Assertions.assertEquals("x5", serviceProperty.unitTestGetProp("run.args.security.path.storage"));

        Assertions.assertEquals(1, propertiesAgent.getListSubscriber().size());

        Assertions.assertEquals(
                new ArrayList<>() {{
                    this.add(new PropertySubscriber().setKey("run.args.security.path.storage"));
                }}.toString(),
                propertiesAgent.getListSubscriber().toString()
        );
        // Как было 7 onUpdate так 7 и осталось
        Assertions.assertEquals(7, xx.c);

        // Обратно подписываюсь
        propertiesAgent.add(String.class,"run.args.security.path.public.key", null,true, null);
        Assertions.assertEquals(
                new ArrayList<>() {{
                    this.add(new PropertySubscriber().setKey("run.args.security.path.storage"));
                    this.add(new PropertySubscriber().setKey("run.args.security.path.public.key"));
                }}.toString(),
                propertiesAgent.getListSubscriber().toString()
        );
        // Пришло 1 onUpdate
        Assertions.assertEquals(8, xx.c);

        // Проверяем исходные значения Property
        Assertions.assertEquals("new", serviceProperty.unitTestGetProp("run.args.security.path.public.key"));
        Assertions.assertEquals("x5", serviceProperty.unitTestGetProp("run.args.security.path.storage"));

        //Мульти обновление штатное
        serviceProperty.setProperty(new HashMapBuilder<String, String>()
                .append("run.args.security.path.public.key", "new2")
                .append("run.args.security.path.storage", "x5")
        );
        Assertions.assertEquals(9, xx.c); // 9 так как обновлено только new2, а x5 не поменял значение


        //Мульти обновление штатное
        serviceProperty.setProperty(new HashMapBuilder<String, String>()
                .append("run.args.security.path.public.key", "new2")
                .append("run.args.security.path.storage", "x6")
        );
        Assertions.assertEquals(10, xx.c);

    }


    static class x2 extends RepositoryPropertiesField implements PropertyUpdateDelegate {

        @SuppressWarnings("all")
        @PropertyName("security.path.storage")
        public String storage = "wef";

        @SuppressWarnings("all")
        @PropertyName("security.path.public.key")
        public String publicKey = "ppbb";

        @Override
        public void onPropertyUpdate(Map<String, String> mapAlias) {
            Util.logConsole(getClass(), mapAlias.toString());
        }
    }

    @Test
    void testEnum() {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        x2 x2 = new x2();

        Map<String, Object> mapPropValue = x2.unitTestGetProperties();
        System.out.println(mapPropValue);

        PropertiesAgent subscribe = serviceProperty.getFactory().getPropertiesAgent(x2, x2, "run.args", true);

        Assertions.assertEquals(2, subscribe.getMapSubscription().size());

        Assertions.assertEquals("security/security.jks", x2.storage);

        serviceProperty.setProperty("run.args.security.path.storage", "x3");
        Assertions.assertEquals("x3", x2.storage);

    }

}