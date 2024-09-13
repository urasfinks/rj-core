package ru.jamsys.core.promise;

import org.jetbrains.annotations.NotNull;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.RepositoryMap;
import ru.jamsys.core.extension.trace.TracePromise;
import ru.jamsys.core.flat.util.UtilJson;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class PromiseRepositoryDebug extends HashMap<String, Object> {

    private final Promise promise;

    private final Map<String, Object> repositoryMap;

    public PromiseRepositoryDebug(Map<String, Object> repositoryMap, Promise promise) {
        this.repositoryMap = repositoryMap;
        this.promise = promise;
    }

    @Override
    public int size() {
        return repositoryMap.size();
    }

    @Override
    public boolean isEmpty() {
        return repositoryMap.isEmpty();
    }

    @Override
    public Object get(Object key) {
        return repositoryMap.get(key);
    }

    @Override
    public boolean containsKey(Object key) {
        return repositoryMap.containsKey(key);
    }

    @Override
    public Object put(String key, Object value) {
        try {
            String postFix = "";
            if (value == null) {
                postFix = " with null value";
            } else if (value.toString().isEmpty()) {
                postFix = " with empty value";
            } else if (value instanceof Map && ((Map<?, ?>) value).isEmpty()) {
                postFix = " with empty map";
            } else if (value instanceof List && ((List<?>) value).isEmpty()) {
                postFix = " with empty list";
            }
            TracePromise<String, ?> trace = new TracePromise<>(
                    "put(" + key + ")" + postFix, UtilJson.toLog(value), null, RepositoryMap.class
            );
            if (promise instanceof PromiseDebug) {
                ((PromiseDebug) promise).getPromiseTask().getActiveTrace().getTaskTrace().add(trace);
            } else {
                promise.getTrace().add(trace);
            }
        } catch (Throwable th) {
            App.error(th);
        }
        return repositoryMap.put(key, value);
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        String postFix = "";
        if (m == null) {
            postFix = " set null value";
        } else if (m.isEmpty()) {
            postFix = " set empty value";
        }
        TracePromise<String, ?> trace = new TracePromise<>(
                "putAll " + postFix, UtilJson.toLog(m), null, RepositoryMap.class
        );
        if (promise instanceof PromiseDebug) {
            ((PromiseDebug) promise).getPromiseTask().getActiveTrace().getTaskTrace().add(trace);
        } else {
            promise.getTrace().add(trace);
        }
        assert m != null;
        repositoryMap.putAll(m);
    }

    @Override
    public Object remove(Object key) {
        return repositoryMap.remove(key);
    }

    @Override
    public void clear() {
        repositoryMap.clear();
    }

    @Override
    public boolean containsValue(Object value) {
        return repositoryMap.containsValue(value);
    }

    @NotNull
    @Override
    public Set<String> keySet() {
        return repositoryMap.keySet();
    }

    @NotNull
    @Override
    public Collection<Object> values() {
        return repositoryMap.values();
    }

    @NotNull
    @Override
    public Set<Entry<String, Object>> entrySet() {
        return repositoryMap.entrySet();
    }

    @Override
    public Object getOrDefault(Object key, Object defaultValue) {
        return repositoryMap.getOrDefault(key, defaultValue);
    }

    @Override
    public Object putIfAbsent(String key, Object value) {
        return repositoryMap.putIfAbsent(key, value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return repositoryMap.remove(key, value);
    }

    @Override
    public boolean replace(String key, Object oldValue, Object newValue) {
        return repositoryMap.replace(key, oldValue, newValue);
    }

    @Override
    public Object replace(String key, Object value) {
        return repositoryMap.replace(key, value);
    }

    @Override
    public Object computeIfAbsent(String key, @NotNull Function<? super String, ?> mappingFunction) {
        return repositoryMap.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public Object computeIfPresent(String key, @NotNull BiFunction<? super String, ? super Object, ?> remappingFunction) {
        return repositoryMap.computeIfPresent(key, remappingFunction);
    }

    @Override
    public Object compute(String key, @NotNull BiFunction<? super String, ? super Object, ?> remappingFunction) {
        return repositoryMap.compute(key, remappingFunction);
    }

    @Override
    public Object merge(String key, @NotNull Object value, @NotNull BiFunction<? super Object, ? super Object, ?> remappingFunction) {
        return repositoryMap.merge(key, value, remappingFunction);
    }

    @Override
    public void forEach(BiConsumer<? super String, ? super Object> action) {
        repositoryMap.forEach(action);
    }

    @Override
    public void replaceAll(BiFunction<? super String, ? super Object, ?> function) {
        repositoryMap.replaceAll(function);
    }


}
