package ru.jamsys.core.component.manager;

import ru.jamsys.core.extension.CheckClassItem;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.util.function.Consumer;

public class EnvelopManagerObject<T extends CheckClassItem> extends ExpirationMsMutableImpl {

    private final String index;

    private final Class<?> classItem;

    private final AbstractManager2<?> abstractManager2;

    private T cache;

    public EnvelopManagerObject(String index, Class<?> classItem, AbstractManager2<?> abstractManager2) {
        this.index = index;
        this.classItem = classItem;
        this.abstractManager2 = abstractManager2;
        setKeepAliveOnInactivityMs(1_000);
    }

    public T get() {
        if (cache == null || isExpired()) {
            cache = abstractManager2.getMapItem(index, classItem);
            active();
        }
        return cache;
    }

    public void accept(Consumer<T> consumer) {
        consumer.accept(get());
    }

}
