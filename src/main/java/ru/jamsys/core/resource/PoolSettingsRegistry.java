package ru.jamsys.core.resource;

import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.sub.PoolSettings;
import ru.jamsys.core.extension.UniqueClassName;
import ru.jamsys.core.extension.UniqueClassNameImpl;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class PoolSettingsRegistry<
        R extends Resource<?, RC> & ResourceCheckException,
        RC extends ResourceArguments
        >
        implements UniqueClassName {

    private final Map<Class<R>, Function<Throwable, Boolean>> fn = new HashMap<>();

    private final Map<String, PoolSettings<R>> registry = new HashMap<>();

    @SuppressWarnings("all")
    public PoolSettings<?> get(Class<R> cls, String ns) {
        String index = UniqueClassNameImpl.getClassNameStatic(cls, ns, App.context);
        return registry.computeIfAbsent(index, s -> {
            return (PoolSettings<R>) new PoolSettings<>(
                    s,
                    cls,
                    new ResourceArguments(ns),
                    fn.computeIfAbsent(cls, rClass -> App.get(rClass).getFatalException())
            );
        });
    }

}
