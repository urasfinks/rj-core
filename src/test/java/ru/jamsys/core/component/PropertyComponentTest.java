package ru.jamsys.core.component;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.*;

import java.util.Map;
import java.util.Set;

class PropertyComponentTest {
    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        //App.main(args); мы не можем стартануть проект, так как запустится keepAlive
        // который будет сбрасывать счётчики tps и тесты будут разваливаться
        App.main(args);
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    public static class XX extends PropertyConnector implements PropertySubscriberNotify {

        int c = 0;

        @Override
        public void onPropertyUpdate(Set<String> updatedProp) {
            c += updatedProp.size();
            System.out.println(updatedProp);
        }
    }

    @Test
    void test() {
        PropertyComponent propertyComponent = App.context.getBean(PropertyComponent.class);
        XX xx = new XX();
        Subscriber subscribe = propertyComponent
                .getSubscriber(xx, xx)
                .subscribe("core.security.path.storage", true)
                .subscribe("core.security.path.storage", true);

        Assertions.assertEquals(1, subscribe.getSubscriptions().size());

        subscribe.subscribe("core.security.path.public.key", true);

        Assertions.assertEquals(2, subscribe.getSubscriptions().size());

        Assertions.assertEquals(2, xx.c);

        propertyComponent.update("core.security.path.storage", "xx");
        Assertions.assertEquals(3, xx.c);

        // Дубликат значения не должен вызывать onPropUpdate
        propertyComponent.update("core.security.path.storage", "xx");
        Assertions.assertEquals(3, xx.c);

        subscribe.unsubscribe();
        Assertions.assertEquals(0, subscribe.getSubscriptions().size());

        // После отписки мы не должны получать уведомления об изменениях
        propertyComponent.update("core.security.path.storage", "x2");
        Assertions.assertEquals(3, xx.c);

        // Обратно подписываемся
        subscribe.subscribe("core.security.path.storage", true);
        // Так как автоматом получим значение при подписке
        Assertions.assertEquals(4, xx.c);

        subscribe.subscribe("core.security.path.public.key", true);
        // Так как автоматом получим значение при подписке
        Assertions.assertEquals(5, xx.c);

        propertyComponent.update("core.security.path.public.key", "x3");
        Assertions.assertEquals(6, xx.c);

        subscribe.unsubscribe("core.security.path.public.key");
        propertyComponent.update("core.security.path.public.key", "x4");
        Assertions.assertEquals(6, xx.c);

        // Проверяем что другие подписки работают
        propertyComponent.update("core.security.path.storage", "x5");
        Assertions.assertEquals(7, xx.c);

        //Мульти обновление без изменений значений
        propertyComponent.update(new HashMapBuilder<String, String>()
                .append("core.security.path.public.key", "x4")
                .append("core.security.path.storage", "x5")
        );
        Assertions.assertEquals(7, xx.c);


        //Мульти обновление c неподписанным
        propertyComponent.update(new HashMapBuilder<String, String>()
                .append("core.security.path.public.key", "new") // Подписки нет, но значение изменено
                .append("core.security.path.storage", "x5") // Подписка есть но значение старое
        );
        Assertions.assertEquals(7, xx.c);

        subscribe.subscribe("core.security.path.public.key", true);
        Assertions.assertEquals(8, xx.c);

        //Мульти обновление штатное
        propertyComponent.update(new HashMapBuilder<String, String>()
                .append("core.security.path.public.key", "new2")
                .append("core.security.path.storage", "x5")
        );
        Assertions.assertEquals(10, xx.c);

        //Мульти обновление штатное
        propertyComponent.update(new HashMapBuilder<String, String>()
                .append("core.security.path.public.key", "new2")
                .append("core.security.path.storage", "x6")
        );
        Assertions.assertEquals(12, xx.c);

    }


    static class x2 extends PropertyConnector implements PropertySubscriberNotify {

        @SuppressWarnings("all")
        @PropertyName("security.path.storage")
        public String storage="wef";

        @SuppressWarnings("all")
        @PropertyName("security.path.public.key")
        public String publicKey="ppbb";

        @Override
        public void onPropertyUpdate(Set<String> updatedProp) {
            System.out.println(updatedProp);
        }
    }

    @Test
    void testEnum() {
        PropertyComponent propertyComponent = App.context.getBean(PropertyComponent.class);
        x2 x2 = new x2();

        Map<String, String> mapPropValue = x2.getMapPropValue();
        System.out.println(mapPropValue);

        Subscriber subscribe = propertyComponent.getSubscriber(x2, x2, "core");

        Assertions.assertEquals(2, subscribe.getSubscriptions().size());

        Assertions.assertEquals("security/security.jks", x2.storage);

        propertyComponent.update("core.security.path.storage", "x3");
        Assertions.assertEquals("x3", x2.storage);

    }

}