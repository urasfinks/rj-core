package ru.jamsys.core.rate.limit.item;

import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.StatisticsFlush;

public interface RateLimitItem extends StatisticsFlush, LifeCycleInterface {

    boolean check();

    int getCount();

    int getMax();

    String getNamespace();

    default void set(int value) {
        //+.max потому что поле в репозитории называется max
        App.get(ServiceProperty.class)
                .computeIfAbsent(getNamespace() + ".max", null)
                .set(value);
    }

}
