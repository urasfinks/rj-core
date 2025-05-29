package ru.jamsys.core.component.manager;

import com.fasterxml.jackson.annotation.JsonValue;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.AbstractManagerElement;
import ru.jamsys.core.extension.builder.HashMapBuilder;

import java.util.function.Consumer;

// Создан для хранения onCreate функционала, что бы в Manager не было утечек builder.
// То есть сам объект ExpiredElement находится в Manager и для двух ManagerConfiguration ссылка будет на один
// элемент. А onCreate же будет два, каждый для своего ManagerConfiguration. GC удаляет ManagerConfiguration и onCreate
// улетает в след за ним. Выросло это всё из того, что хотелось в одном месте сконфигурировать объект, а в другом
// просто получить, ничего не зная о onCreate. Ранее onCreate жил в Manager.mapBuilder, но не удалялись и текли
// соответственно. Поэтому разместили onCreate в ManagerConfiguration

public class ManagerConfiguration<T extends AbstractManagerElement> {

    private final Class<T> cls;

    private final String key;

    private final Manager manager;

    private long nextUpdate;

    private T cache;

    Consumer<T> onCreate;

    private ManagerConfiguration(Class<T> cls, String key, Manager manager, Consumer<T> onCreate) {
        this.cls = cls;
        this.key = key;
        this.manager = manager;
        this.onCreate = onCreate;
    }

    @JsonValue
    public Object getValue() {
        return new HashMapBuilder<String, Object>()
                .append("hashCode", Integer.toHexString(hashCode()))
                .append("cls", cls)
                .append("key", key)
                .append("reference", isAlive() ? manager.get(cls, key, onCreate) : null);
    }

    public boolean isAlive() {
        return manager.contains(cls, key);
    }

    public T get() {
        long l = System.currentTimeMillis();
        if (l > nextUpdate) {
            cache = manager.get(cls, key, onCreate);
            nextUpdate = cache.getExpiryRemainingMs() + l;
        }
        // TODO: почистить markActive внутри реализаций, потому что всё должно работать через Manager
        cache.markActive();
        return cache;
    }

    public void execute(Consumer<T> managerElement) {
        T t = get();
        if (t != null) {
            managerElement.accept(t);
        }
    }

    public void executeIfAlive(Consumer<T> managerElement) {
        if (!isAlive()) {
            return;
        }
        execute(managerElement);
    }

    public static <R extends AbstractManagerElement, S extends AbstractManagerElement> ManagerConfiguration<R> getInstance(
            Class<S> cls,
            String ns
    ) {
        return getInstance(cls, ns, null);
    }

    public static <R extends AbstractManagerElement, S extends AbstractManagerElement> ManagerConfiguration<R> getInstance(
            Class<S> cls,
            String ns,
            Consumer<R> onCreate
    ) {
        @SuppressWarnings("unchecked")
        Class<R> newCls = (Class<R>) cls;
        return new ManagerConfiguration<>(newCls, ns, App.get(Manager.class), onCreate);
    }

}
