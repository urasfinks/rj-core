package ru.jamsys.core.extension;

import org.springframework.context.ApplicationContext;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutable;

import java.lang.reflect.Constructor;

public interface ManagerElement extends ExpirationMsMutable, StatisticsFlush, LifeCycleInterface {
    default void helper() {
    }

    static <T extends ManagerElement> Manager.Configuration<T> getConfigure(Class<T> cls, String ns) {
        return getConfigure(App.context, cls, ns);
    }

    static <T extends ManagerElement> Manager.Configuration<T> getConfigure(
            ApplicationContext applicationContext,
            Class<T> cls,
            String ns
    ) {
        return App.get(Manager.class, applicationContext).configureGeneric(
                cls,
                ns,
                s -> {
                    try {
                        Constructor<T> constructor = cls.getConstructor(String.class);
                        return constructor.newInstance(s);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to instantiate " + cls.getName() + "(String)", e);
                    }
                }
        );
    }
}
