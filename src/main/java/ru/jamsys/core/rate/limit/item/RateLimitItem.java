package ru.jamsys.core.rate.limit.item;

import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.StatisticsFlush;

public interface RateLimitItem extends StatisticsFlush, LifeCycleInterface {

    boolean check();

    int get();

    int max();

    String getKey();

    default void set(int value) {
        App.get(ServiceProperty.class)
                .computeIfAbsent(getKey() + ".max", null)
                .set(value);
    }

}
