package ru.jamsys.core.rate.limit.item;

import org.springframework.context.ApplicationContext;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.StatisticsFlush;

public interface RateLimitItem extends StatisticsFlush {

    boolean check();

    int get();

    int max();

    String getKey();

    default void set(int value) {
        App.get(ServiceProperty.class)
                .get(getKey(), 0, getClass().getName())
                .set(value, getClass().getName());
    }

    default void set(ApplicationContext applicationContext, int value) {
        applicationContext
                .getBean(ServiceProperty.class)
                .get(getKey(), 0, getClass().getName())
                .set(value, getClass().getName());
    }

}
