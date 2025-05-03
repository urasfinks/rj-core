package ru.jamsys.core.rate.limit.item;

import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.ManagerElement;

public interface RateLimitItem extends ManagerElement {

    String getNs();

    boolean check();

    int getCount();

    int getMax();

    String getPropertyKey();

    default void setMax(int value) {
        //+.max потому что поле в репозитории называется max
        App.get(ServiceProperty.class)
                .computeIfAbsent(getPropertyKey() + ".max", null)
                .set(value);
    }

}
