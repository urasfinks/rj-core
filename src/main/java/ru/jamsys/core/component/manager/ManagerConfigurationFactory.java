package ru.jamsys.core.component.manager;

import ru.jamsys.core.App;
import ru.jamsys.core.extension.ManagerElement;

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
        return App.get(Manager.class).getManagerConfigurationGeneric(
                cls,
                ns,
                ns1 -> {
                    try {
                        Constructor<?> c = cls.getConstructor(String.class);
                        @SuppressWarnings("unchecked")
                        R instance = (R) c.newInstance(ns1);
                        if (onCreate != null) {
                            onCreate.accept(instance);
                        }
                        return instance;
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to instantiate " + cls + "(String)", e);
                    }
                }
        );
    }

}
