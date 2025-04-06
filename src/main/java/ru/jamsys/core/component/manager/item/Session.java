package ru.jamsys.core.component.manager.item;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.ManagerExpiration;
import ru.jamsys.core.extension.ClassEquals;
import ru.jamsys.core.extension.LifeCycleInterface;
import ru.jamsys.core.extension.StatisticsFlush;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImplAbstractLifeCycle;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class Session<K, V>
        extends ExpirationMsMutableImplAbstractLifeCycle
        implements
        Map<K, V>,
        StatisticsFlush,
        LifeCycleInterface,
        ClassEquals {

    @Getter
    private final Map<K, V> map = new ConcurrentHashMap<>(); // Основная карта, в которой хранятся сессионные данные

    // Брокер нужен, что бы срабатывал механизм onDrop, что бы мы подчищали основную карту map
    private final Expiration<DisposableExpirationMsImmutableEnvelope> expiration;

    // Помещаем в эту карту элементы из брокера, что бы можно удалить из брокера ключ, не дожидаясь его onDrop
    private final Map<K, DisposableExpirationMsImmutableEnvelope<K>> mapExpiration = new ConcurrentHashMap<>();

    @Getter
    private final String key;

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
        resetTimer(k);
        return map.get(key);
    }

    private void resetTimer(K key) {
        // Нам тут вообще не интересна многопоточность, одновременно обновят таймер ну и прекрасно
        DisposableExpirationMsImmutableEnvelope<K> env = mapExpiration.remove(key);
        if (env != null) {
            env.doNeutralized();
            // Это значит, что когда сработает onDrop у Expiration - функция отработает в холостую
        }
        // Добавляем новый таймер
        @SuppressWarnings("all")
        DisposableExpirationMsImmutableEnvelope newEnv = mapExpiration
                .computeIfAbsent(key, key1 -> new DisposableExpirationMsImmutableEnvelope<>(
                        key1,
                        getKeepAliveOnInactivityMs()
                ));
        expiration.add(newEnv);
    }

    public V computeIfAbsent(K key, @NotNull Function<? super K, ? extends V> mappingFunction) {
        resetTimer(key);
        return map.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public V put(K key, V value) {
        resetTimer(key);
        return map.put(key, value);
    }

    public Session(String key, int keepAliveOnInactivityMs) {
        this.key = key;
        setKeepAliveOnInactivityMs(keepAliveOnInactivityMs);
        this.expiration = App.get(ManagerExpiration.class).get(
                key,
                DisposableExpirationMsImmutableEnvelope.class,
                env -> {
                    @SuppressWarnings("unchecked")
                    K key1 = (K) env.getValue();
                    if (key1 != null) {
                        map.remove(key1);
                        mapExpiration.remove(key1);
                    }
                }
        );
    }

    @Override
    public V remove(Object key) {
        @SuppressWarnings("unchecked")
        K k = (K) key;
        return map.remove(k);
        // Если в карте ничего нет, ну вызовется onDrop - ну и ладно, а если добавят поверх существующего
        // перезатрётся таймер
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

    @Override
    public long getLastActivityMs() {
        return 0;
    }

    @Override
    public long getKeepAliveOnInactivityMs() {
        return 0;
    }

    @Override
    public void setStopTimeMs(Long timeMs) {

    }

    @Override
    public Long getStopTimeMs() {
        return 0L;
    }

    @Override
    public boolean classEquals(Class<?> classItem) {
        return true;
    }

    @Override
    public void runOperation() {

    }

    @Override
    public void shutdownOperation() {

    }

    public List<Statistic> flushAndGetStatistic(
            Map<String, String> parentTags,
            Map<String, Object> parentFields,
            AtomicBoolean threadRun
    ) {
        List<Statistic> result = new ArrayList<>();
        result.add(new Statistic(parentTags, parentFields)
                .addField("size", size())
                .addField("sizeExpiration", sizeExpiration())
        );
        return result;
    }

}
