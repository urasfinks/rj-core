package ru.jamsys.core.extension.expiration;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.extension.ManagerElement;
import ru.jamsys.core.component.manager.item.log.DataHeader;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImplAbstractLifeCycle;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

// Временная карта - хранит элементы keepAliveOnInactivityMs, далее удаляет
// если в течении этого времени не было обращений к ним.
// В случае обращения таймер сбрасывается опять на keepAliveOnInactivityMs и объект продолжает жить в карте
// Нельзя устанавливать произвольное время для разных элементов, у всех время жизни одно

@Getter
public class ExpirationMap<K, V>
        extends ExpirationMsMutableImplAbstractLifeCycle
        implements
        Map<K, V>,
        ManagerElement {

    private final String key;

    private final Map<K, V> mainMap = new ConcurrentHashMap<>(); // Основная карта, в которой хранятся сессионные данные

    @Getter
    private final Map<K, DisposableExpirationMsImmutableEnvelope<K>> expirationMap = new ConcurrentHashMap<>();

    @SuppressWarnings("all")
    private final Manager.Configuration<ExpirationList> expirationListConfiguration;

    public ExpirationMap(String key, int keepAliveOnInactivityMs) {
        this.key = key;
        setKeepAliveOnInactivityMs(keepAliveOnInactivityMs);
        expirationListConfiguration = App.get(Manager.class).configure(
                ExpirationList.class,
                key,
                (key1) -> new ExpirationList<>(key1, env -> {
                    @SuppressWarnings("unchecked")
                    K key2 = (K) env.getValue();
                    if (key2 != null) {
                        mainMap.remove(key2);
                        expirationMap.remove(key2);
                    }
                })
        );
    }

    public int size() {
        return mainMap.size();
    }

    @Override
    public V put(K key, V value) {
        resetTimer(key);
        return mainMap.put(key, value);
    }

    @Override
    public V get(Object key) {
        @SuppressWarnings("unchecked")
        K k = (K) key;
        resetTimer(k);
        return mainMap.get(key);
    }

    @SuppressWarnings("unchecked")
    private void resetTimer(K key) {
        // Нам тут вообще не интересна многопоточность, одновременно обновят таймер ну и прекрасно
        DisposableExpirationMsImmutableEnvelope<K> env = expirationMap.remove(key);
        if (env != null) {
            env.doNeutralized();
            // Это значит, что когда сработает onDrop у Expiration - функция отработает в холостую
        }
        // Добавляем новый таймер
        @SuppressWarnings("all")
        DisposableExpirationMsImmutableEnvelope newEnv = expirationMap
                .computeIfAbsent(key, key1 -> new DisposableExpirationMsImmutableEnvelope<>(
                        key1,
                        getKeepAliveOnInactivityMs()
                ));
        expirationListConfiguration.get().add(newEnv);
    }

    public V computeIfAbsent(K key, @NotNull Function<? super K, ? extends V> mappingFunction) {
        resetTimer(key);
        return mainMap.computeIfAbsent(key, mappingFunction);
    }

    // Смысл remove ExpirationMap и ExpirationList совершенно разный, если ExpirationList.remove() создан для того,
    // что бы не вызвался onDrop, так как операция была выполнена (к примеру),
    // то тут ExpirationMap.remove() нет никакой логики onDrop, потому что удаление по времени это просто очистка
    // памяти, не связанная с бизнес-логикой.
    @Override
    public V remove(Object key) {
        @SuppressWarnings("unchecked")
        K k = (K) key;
        return mainMap.remove(k);
        // Если в карте ничего нет, ну вызовется onDrop - ну и ладно, а если добавят поверх существующего
        // перезатрётся таймер
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        m.forEach(this::put);
    }

    @Override
    public void clear() {
        mainMap.clear();
        expirationMap.clear();
    }

    @NotNull
    @Override
    public Set<K> keySet() {
        return mainMap.keySet();
    }

    @NotNull
    @Override
    public Collection<V> values() {
        return mainMap.values();
    }

    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return mainMap.entrySet();
    }

    @Override
    public boolean isEmpty() {
        return mainMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return mainMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return mainMap.containsValue(value);
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
    public void runOperation() {

    }

    @Override
    public void shutdownOperation() {

    }

    public List<DataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        List<DataHeader> result = new ArrayList<>();
        result.add(new DataHeader()
                .setBody(key)
                .addHeader("mainSize", size())
                .addHeader("expirationSize", expirationMap.size())
        );
        return result;
    }

}
