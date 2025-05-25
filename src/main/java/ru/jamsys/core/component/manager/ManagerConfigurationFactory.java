package ru.jamsys.core.component.manager;

import ru.jamsys.core.App;
import ru.jamsys.core.extension.ManagerElement;
import ru.jamsys.core.extension.exception.ForwardException;

import java.lang.reflect.Constructor;
import java.util.function.Consumer;

public interface ManagerConfigurationFactory {

    static <R extends ManagerElement, S extends ManagerElement> ManagerConfiguration<R> get(
            Class<S> cls,
            String ns
    ) {
        return get(cls, ns, null);
    }

    static <R extends ManagerElement, S extends ManagerElement> ManagerConfiguration<R> get(
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
