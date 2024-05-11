package ru.jamsys.core.component.item;

import lombok.Getter;
import ru.jamsys.core.extension.Closable;
import ru.jamsys.core.extension.KeepAlive;
import ru.jamsys.core.extension.StatisticsFlush;
import ru.jamsys.core.extension.addable.AddToMap;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.time.mutable.ExpirationMsMutableImpl;
import ru.jamsys.core.statistic.time.mutable.ExpirationMsMutableEnvelope;
import ru.jamsys.core.util.UtilRisc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

// Для задач когда надо прихранить какие-либо данные на время по ключу


@Getter
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class Session<K, TEO>
        extends ExpirationMsMutableImpl
        implements StatisticsFlush, KeepAlive, Closable, AddToMap<K, ExpirationMsMutableEnvelope<TEO>> {

    final Map<K, ExpirationMsMutableEnvelope<TEO>> map = new ConcurrentHashMap<>();

    private final String index;

    public Session(String index) {
        this.index = index;
    }

    @Override
    public void add(K key, ExpirationMsMutableEnvelope<TEO> value) {
        map.computeIfAbsent(key, _ -> value).active();
    }

    public ExpirationMsMutableEnvelope<TEO> add(K key, TEO value, long curTime, long timeoutMs) {
        ExpirationMsMutableEnvelope<TEO> expirationMsMutableEnvelope = new ExpirationMsMutableEnvelope<>(value);
        expirationMsMutableEnvelope.setKeepAliveOnInactivityMs(timeoutMs);
        // Что бы не переопределить активность
        map.computeIfAbsent(key, _ -> expirationMsMutableEnvelope).setLastActivityMs(curTime);
        return expirationMsMutableEnvelope;
    }

    public ExpirationMsMutableEnvelope<TEO> add(K key, TEO value, long timeoutMs) {
        return add(key, value, System.currentTimeMillis(), timeoutMs);
    }

    public TEO get(K key) {
        ExpirationMsMutableEnvelope<TEO> expirationMsMutableEnvelope = map.get(key);
        if (expirationMsMutableEnvelope != null) {
            expirationMsMutableEnvelope.active();
            return expirationMsMutableEnvelope.getValue();
        }
        return null;
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isThreadRun) {
        List<Statistic> result = new ArrayList<>();
        result.add(new Statistic(parentTags, parentFields)
                .addField("MapSize", map.size())
        );
        return result;
    }

    @Override
    public void keepAlive(AtomicBoolean isThreadRun) {
        UtilRisc.forEach(isThreadRun, map, (K key, ExpirationMsMutableEnvelope<TEO> value) -> {
            if (value.isExpired()) {
                map.remove(key);
            }
        });
    }

    @Override
    public void close() {
        map.clear();
    }

}
