package ru.jamsys.cache;

import lombok.Setter;
import ru.jamsys.extension.KeepAlive;
import ru.jamsys.extension.StatisticsCollector;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.util.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

// Для задач когда надо прихранить какие-либо данные на время по ключу
// Предположим есть номер транзакции, в рамках которой мы хотим прихранить данные
public class MapExpired<K, V> implements StatisticsCollector, KeepAlive {

    final Map<K, TimeEnvelope<V>> map = new ConcurrentHashMap<>();

    @Setter
    private Consumer<V> onExpired;

    public boolean add(K key, V value, long curTime, long timeoutMs) {
        if (!map.containsKey(key)) {
            TimeEnvelope<V> timeEnvelope = new TimeEnvelope<>(value);
            timeEnvelope.setKeepAliveOnInactivityMs(timeoutMs);
            timeEnvelope.setLastActivity(curTime);
            map.put(key, timeEnvelope);
            return true;
        }
        return false;
    }

    public boolean add(K key, V value, long timeoutMs) {
        return add(key, value, System.currentTimeMillis(), timeoutMs);
    }

    public V get(K key) {
        TimeEnvelope<V> timeEnvelope = map.get(key);
        if (timeEnvelope != null && !timeEnvelope.isExpired()) {
            timeEnvelope.stop();
            return timeEnvelope.getValue();
        }
        return null;
    }

    public Map<K, TimeEnvelope<V>> get() {
        return map;
    }

    @SafeVarargs
    static <K> K[] getEmptyType(K... array) {
        return Arrays.copyOf(array, 0);
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isRun) {
        List<Statistic> result = new ArrayList<>();
        result.add(new Statistic(parentTags, parentFields).addField("size", map.size()));
        return result;
    }

    @Override
    public void keepAlive(AtomicBoolean isRun) {
        long curMs = System.currentTimeMillis();
        Util.riskModifierMap(isRun, map, getEmptyType(), (K key, TimeEnvelope<V> value) -> {
            if (value.isExpired(curMs)) { //Если никто ни разу не получил эти данные
                map.remove(key);
                if (onExpired != null) {
                    onExpired.accept(value.getValue());
                }
            } else if (value.isStop()) {
                map.remove(key);
            }
        });
    }
}
