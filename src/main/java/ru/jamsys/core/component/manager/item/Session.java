package ru.jamsys.core.component.manager.item;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.ManagerBroker;
import ru.jamsys.core.extension.UniqueClassNameImpl;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class Session<K, V> implements Map<K, V> {

    @Getter
    private final Map<K, V> map = new ConcurrentHashMap<>();

    private final Map<K, DisposableExpirationMsImmutableEnvelope<K>> mapExpiration = new ConcurrentHashMap<>();

    private final long keepAliveOnInactivityMs;

    Broker<K> broker;

    public int size() {
        return map.size();
    }

    public int sizeExpiration() {
        return mapExpiration.size();
    }

    @Override
    public V get(Object key) {
        @SuppressWarnings("unchecked")
        K k = (K) key;
        if (map.containsKey(k)) {
            broker.remove(mapExpiration.get(k)); //Старый таймер останавливаем удаления
            mapExpiration.put(k, broker.add(k, keepAliveOnInactivityMs)); // Устанавливаем новый таймер
        }
        return map.get(key);
    }

    public V computeIfAbsent(K key, @NotNull Function<? super K, ? extends V> mappingFunction) {
        if (!map.containsKey(key)) {
            put(key, mappingFunction.apply(key));
        }
        return map.get(key);
    }

    @Override
    public V put(K key, V value) {
        mapExpiration.put(key, broker.add(key, keepAliveOnInactivityMs));
        return map.put(key, value);
    }

    public Session(String index, Class<K> clsKey, long keepAliveOnInactivityMs) {
        this.keepAliveOnInactivityMs = keepAliveOnInactivityMs;
        broker = App.get(ManagerBroker.class).initAndGet(
                UniqueClassNameImpl.getClassNameStatic(Session.class, index),
                clsKey,
                (k) -> {
                    map.remove(k);
                    mapExpiration.remove(k);
                }
        );
    }

    @Override
    public V remove(Object key) {
        @SuppressWarnings("unchecked")
        K k = (K) key;
        if (map.containsKey(k)) {
            broker.remove(mapExpiration.get(k)); //Старый таймер останавливаем удаления
        }
        mapExpiration.remove(key);
        return map.remove(k);
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        m.forEach(this::put);
    }

    @Override
    public void clear() {
        map.clear();
        mapExpiration.clear();
    }

    @NotNull
    @Override
    public Set<K> keySet() {
        return map.keySet();
    }

    @NotNull
    @Override
    public Collection<V> values() {
        return map.values();
    }

    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

}
