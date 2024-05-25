package ru.jamsys.core.component.manager.sub;

import ru.jamsys.core.extension.CheckClassItem;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.util.function.Consumer;

// C  - Collection
// CE - CollectionElement
// BA - BuilderArgument

public class ManagerElement<C extends CheckClassItem, BA> extends ExpirationMsMutableImpl {

    private final String index;

    private final Class<?> classItem;

    private final AbstractManager<?, BA> abstractManager;

    private final BA customArgument;

    private C cache;

    public ManagerElement(
            String index,
            Class<?> classItem,
            AbstractManager<?, BA> abstractManager,
            BA customArgument
    ) {
        this.index = index;
        this.classItem = classItem;
        this.abstractManager = abstractManager;
        this.customArgument = customArgument;
        setKeepAliveOnInactivityMs(1_000);
    }

    public C get() {
        if (cache == null || isExpiredWithoutStop()) {
            cache = abstractManager.getManagerElement(index, classItem, customArgument);
            active();
        }
        return cache;
    }

    public void accept(Consumer<C> consumer) {
        consumer.accept(get());
    }

}
