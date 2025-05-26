package ru.jamsys.core.component.manager;

import com.fasterxml.jackson.annotation.JsonValue;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.ManagerElement;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;

import java.lang.reflect.Constructor;
import java.util.function.Consumer;

public class ManagerConfiguration<T extends ManagerElement> {

    private final Class<T> cls;

    private final String key;

    private final Manager manager;

    private long nextUpdate;

    private T cache;

    private ManagerConfiguration(Class<T> cls, String key, Manager manager) {
        this.cls = cls;
        this.key = key;
        this.manager = manager;
    }

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

    public static <R extends ManagerElement, S extends ManagerElement> ManagerConfiguration<R> getInstance(
            Class<S> cls,
            String ns
    ) {
        return getInstance(cls, ns, null);
    }

    public static <R extends ManagerElement, S extends ManagerElement> ManagerConfiguration<R> getInstance(
            Class<S> cls,
            String ns,
            Consumer<R> onCreate
    ) {
        Manager manager = App.get(Manager.class);
        manager.registerBuilder(
                cls,
                ns,
                ns1 -> {
                    try {
                        Constructor<?> c = cls.getConstructor(String.class);
                        @SuppressWarnings("unchecked")
                        R instance = (R) c.newInstance(ns1);
                        if (onCreate != null) {
                            // Настройки которые можно сделать с экземпляром в момент создания принято в методах
                            // называть с ключевого слова setup (setupOnExpired(...)) так же должны следовать сразу за
                            // конструктором
                            onCreate.accept(instance);
                        }
                        return instance;
                    } catch (Throwable th) {
                        throw new ForwardException("Failed to instantiate " + cls + "(String)", th);
                    }
                });
        @SuppressWarnings("unchecked")
        Class<R> newCls = (Class<R>) cls;
        return new ManagerConfiguration<>(newCls, ns, manager);
    }

}
