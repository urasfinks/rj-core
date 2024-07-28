package ru.jamsys.core.extension.property;

import lombok.Getter;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.functional.Procedure;
import ru.jamsys.core.flat.util.UtilRisc;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

// Считывает все значения ключей по NS, важно, что бы значения были одного типа, если типы разные - используйте string
// При изменении какого-либо ключа - вызывается onUpdate, как вы напишите обработчик - уже ваша проблема)

public class PropertiesMap<T> implements LifeCycleInterface {

    private final Map<String, Property<T>> map = new LinkedHashMap<>();

    private final Procedure onUpdate;

    @Getter
    private final String ns;

    public void onUpdate(T oldValue, T newValue) {
        if (onUpdate != null) {
            onUpdate.run();
        }
    }

    public PropertiesMap(ServiceProperty serviceProperty, String ns, Class<T> cls, Procedure onUpdate) {
        this.onUpdate = onUpdate;
        this.ns = ns;
        Map<String, String> prop = serviceProperty.getProp();
        UtilRisc.forEach(null, prop, (key, value) -> {
            if (key.startsWith(ns + ".")) {
                map.computeIfAbsent(
                        key,
                        _ -> serviceProperty.getFactory().getProperty(key, cls, value, this::onUpdate)
                );
            }
        });

    }

    public Property<T> get(String key) {
        return map.get(key);
    }

    public Property<T> getWithoutNs(String key) {
        return map.get(ns + "." + key);
    }

    public Set<String> getKeySet() {
        return map.keySet();
    }

    public Set<String> getKeySetWithoutNs() {
        Set<String> result = new LinkedHashSet<>();
        map.keySet().forEach(s -> result.add(s.substring(ns.length() + 1)));
        return result;
    }

    @Override
    public void run() {
        map.forEach((_, property) -> property.run());
    }

    @Override
    public void shutdown() {
        map.forEach((_, property) -> property.shutdown());
    }

}
