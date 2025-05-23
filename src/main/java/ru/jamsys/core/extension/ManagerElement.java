package ru.jamsys.core.extension;

import org.springframework.context.ApplicationContext;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

import java.lang.reflect.Constructor;
import java.util.function.Consumer;

public interface ManagerElement extends ExpirationMsMutable, StatisticsFlush, LifeCycleInterface {

    default void helper() {
    }

    static <R extends ManagerElement, S extends ManagerElement> Manager.Configuration<R> getConfigure(
            Class<S> cls,
            String ns
    ) {
        return getConfigure(App.context, cls, ns, null);
    }

    static <R extends ManagerElement, S extends ManagerElement> Manager.Configuration<R> getConfigure(
            Class<S> cls,
            String ns,
            Consumer<R> onCreate
    ) {
        return getConfigure(App.context, cls, ns, onCreate);
    }

    static <R extends ManagerElement, S extends ManagerElement> Manager.Configuration<R> getConfigure(
            ApplicationContext applicationContext,
            Class<S> cls,
            String ns,
            Consumer<R> onCreate
    ) {
        return App.get(Manager.class, applicationContext).configureGeneric(
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
