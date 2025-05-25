package ru.jamsys.core.component.manager;

import com.fasterxml.jackson.annotation.JsonValue;
import ru.jamsys.core.extension.ManagerElement;
import ru.jamsys.core.extension.builder.HashMapBuilder;

import java.util.function.Consumer;

public class ManagerConfiguration<T extends ManagerElement> {

    private final Class<T> cls;

    private final String key;

    private final Manager manager;

    private long nextUpdate;

    private T cache;

    public ManagerConfiguration(Class<T> cls, String key, Manager manager) {
        this.cls = cls;
        this.key = key;
        this.manager = manager;
    }

    @SuppressWarnings("all")
    // Получить элемент преобразованные по типу дженерика
//    public <X> X getGeneric() {
//        @SuppressWarnings("unchecked")
//        X t = (X) manager.get(cls, key);
//        return t;
//    }

    @JsonValue
    public Object getValue() {
        return new HashMapBuilder<String, Object>()
                .append("hashCode", Integer.toHexString(hashCode()))
                .append("cls", cls)
                .append("key", key)
                .append("reference", isAlive() ? manager.get(cls, key) : null);
    }

    public boolean isAlive() {
        return manager.contains(cls, key);
    }

    public T get() {
        long l = System.currentTimeMillis();
        if (l > nextUpdate) {
            cache = manager.get(cls, key);
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

}
