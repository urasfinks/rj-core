package ru.jamsys.core.component.manager;

import ru.jamsys.core.extension.CheckClassItem;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.util.function.Consumer;

public class ManagerElement<T extends CheckClassItem> extends ExpirationMsMutableImpl {

    private final String index;

    private final Class<?> classItem;

    private final AbstractManager<?> abstractManager;

    private T cache;

    public ManagerElement(String index, Class<?> classItem, AbstractManager<?> abstractManager) {
        this.index = index;
        this.classItem = classItem;
        this.abstractManager = abstractManager;
        setKeepAliveOnInactivityMs(1_000);
    }

    public T get() {
        if (cache == null || isExpired()) {
            cache = abstractManager.getManagerElement(index, classItem);
            active();
        }
        return cache;
    }

    public void accept(Consumer<T> consumer) {
        consumer.accept(get());
    }

}
