package ru.jamsys.core.component.item;

import lombok.Getter;
import ru.jamsys.core.extension.Closable;
import ru.jamsys.core.extension.KeepAlive;
import ru.jamsys.core.extension.StatisticsFlush;
import ru.jamsys.core.extension.addable.AddToMap;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.time.mutable.ExpiredMsMutableImpl;
import ru.jamsys.core.statistic.time.mutable.ExpiredMsMutableEnvelope;
import ru.jamsys.core.util.UtilRisc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

// Для задач когда надо прихранить какие-либо данные на время по ключу


@SuppressWarnings({"unused", "UnusedReturnValue"})
public class Session<K, TEO>
        extends ExpiredMsMutableImpl
        implements StatisticsFlush, KeepAlive, Closable, AddToMap<K, ExpiredMsMutableEnvelope<TEO>> {

    @Getter
    final Map<K, ExpiredMsMutableEnvelope<TEO>> map = new ConcurrentHashMap<>();

    private final String index;

    public Session(String index) {
        this.index = index;
    }

    @Override
    public void add(K key, ExpiredMsMutableEnvelope<TEO> value) {
        map.computeIfAbsent(key, s -> value).active();
    }

    public ExpiredMsMutableEnvelope<TEO> add(K key, TEO value, long curTime, long timeoutMs) {
        ExpiredMsMutableEnvelope<TEO> expiredMsMutableEnvelope = new ExpiredMsMutableEnvelope<>(value);
        expiredMsMutableEnvelope.setKeepAliveOnInactivityMs(timeoutMs);
        expiredMsMutableEnvelope.setLastActivityMs(curTime);
        add(key, expiredMsMutableEnvelope);
        return expiredMsMutableEnvelope;
    }

    public ExpiredMsMutableEnvelope<TEO> add(K key, TEO value, long timeoutMs) {
        return add(key, value, System.currentTimeMillis(), timeoutMs);
    }

    public TEO get(K key) {
        ExpiredMsMutableEnvelope<TEO> expiredMsMutableEnvelope = map.get(key);
        if (expiredMsMutableEnvelope != null) {
            expiredMsMutableEnvelope.active();
            return expiredMsMutableEnvelope.getValue();
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
        UtilRisc.forEach(isThreadRun, map, (K key, ExpiredMsMutableEnvelope<TEO> value) -> {
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
