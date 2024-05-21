package ru.jamsys.core.extension;

import ru.jamsys.core.component.ClassFinderComponent;

import java.util.Map;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public interface Property<Key> {

    Map<Key, Object> getMapProperty();

    default <R> R setProperty(Key key, R obj) {
        @SuppressWarnings("unchecked")
        R result = (R) getMapProperty().computeIfAbsent(key, _ -> obj);
        return result;
    }

    default <R> R getProperty(Key key, Class<R> cls, R def) {
        Object o = getMapProperty().get(key);
        if (o != null && ClassFinderComponent.instanceOf(o.getClass(), cls)) {
            @SuppressWarnings("unchecked")
            R r = (R) o;
            return r;
        }
        return def;
    }

    default <R> R getProperty(Key key, Class<R> cls) {
        return getProperty(key, cls, null);
    }

    default boolean isProperty(Key key) {
        return getMapProperty().containsKey(key);
    }

}
