package ru.jamsys.core.resource;

import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.sub.PoolSettings;
import ru.jamsys.core.extension.ClassName;
import ru.jamsys.core.extension.ClassNameImpl;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class PoolSettingsRegistry<
        R extends Resource<?, RC> & ResourceCheckException,
        RC extends NamespaceResourceConstructor
        >
        implements ClassName {

    private final Map<Class<R>, Function<Throwable, Boolean>> fn = new HashMap<>();

    private final Map<String, PoolSettings<R>> registry = new HashMap<>();

    @SuppressWarnings("all")
    public PoolSettings<?> get(Class<R> cls, String ns) {
        String index = ClassNameImpl.getClassNameStatic(cls, ns, App.context);
        return registry.computeIfAbsent(index, s -> {
            return (PoolSettings<R>) new PoolSettings<>(
                    s,
                    cls,
                    new NamespaceResourceConstructor(ns),
                    fn.computeIfAbsent(cls, rClass -> App.get(rClass).getFatalException())
            );
        });
    }

}
