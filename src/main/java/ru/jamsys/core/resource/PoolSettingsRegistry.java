package ru.jamsys.core.resource;

import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.sub.PoolSettings;
import ru.jamsys.core.extension.CascadeName;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Component
public class PoolSettingsRegistry<
        R extends Resource<?, RC> & ResourceCheckException,
        RC extends ResourceArguments
        >
        implements CascadeName {

    private final Map<Class<R>, Function<Throwable, Boolean>> fn = new ConcurrentHashMap<>();

    private final Map<String, PoolSettings<R>> registry = new ConcurrentHashMap<>();

    @SuppressWarnings("all")
    public PoolSettings<?> get(Class<R> cls, String ns) {
        return registry.computeIfAbsent(getCascadeName(ns, cls), key -> {
            return (PoolSettings<R>) new PoolSettings<>(
                    key,
                    cls,
                    new ResourceArguments(ns),
                    fn.computeIfAbsent(cls, rClass -> App.get(rClass).getFatalException())
            );
        });
    }

    @Override
    public String getKey() {
        return null;
    }

    @Override
    public CascadeName getParentCascadeName() {
        return App.cascadeName;
    }

}
