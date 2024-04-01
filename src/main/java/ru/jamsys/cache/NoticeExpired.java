package ru.jamsys.cache;

import lombok.Setter;
import ru.jamsys.extension.KeepAlive;
import ru.jamsys.extension.StatisticsCollector;
import ru.jamsys.statistic.Statistic;
import ru.jamsys.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

// Для задач, когда надо сформировать ошибки, если какие либо задачи не исполнились
// Всегда есть лаг срабатывания onExpired, не будет такого, что моментально тик в тик сработает функция
// Ускорить можно путём более частого вызова keepAlive

public class NoticeExpired<T> implements StatisticsCollector, KeepAlive {

    private final Map<Long, ConcurrentLinkedQueue<TimeEnvelope<T>>> map = new ConcurrentHashMap<>();

    private final AtomicLong itemSize = new AtomicLong(0);

    @Setter
    private Consumer<T> onExpired;

    public TimeEnvelope<T> add(T obj, long curTime, int timeOutMs) {
        long timeMsExpired = curTime + timeOutMs;
        //Отбрасываем миллисекунды, что бы в карте не было слишком много разных значений
        //А то теряется весь смысл ускорения
        timeMsExpired = Util.zeroLastNDigits(timeMsExpired, 3);
        //If the key was not present in the map, it maps the passed value to the key and returns null.
        if (!map.containsKey(timeMsExpired)) { //Что бы лишний раз не создавать ConcurrentLinkedQueue при пробе putIfAbsent
            map.putIfAbsent(timeMsExpired, new ConcurrentLinkedQueue<>());
        }

        TimeEnvelope<T> timeEnvelope = new TimeEnvelope<>(obj);
        timeEnvelope.setKeepAliveOnInactivityMs(timeOutMs);
        timeEnvelope.setLastActivity(curTime);

        itemSize.incrementAndGet();
        map.get(timeMsExpired).add(timeEnvelope);
        return timeEnvelope;
    }

    public TimeEnvelope<T> add(T obj, int timeOutMs) {
        return add(obj, System.currentTimeMillis(), timeOutMs);
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isRun) {
        List<Statistic> result = new ArrayList<>();
        result.add(new Statistic(parentTags, parentFields)
                .addField("BucketSize", map.size())
                .addField("ItemSize", itemSize.get())
        );
        return result;
    }

    public void keepAlive(AtomicBoolean isRun, long curTimeMs) {
        //TODO: переделать через отсортированный список времён
        Util.riskModifierMap(isRun, map, new Long[0], (Long timeMs, ConcurrentLinkedQueue<TimeEnvelope<T>> queue) -> {
            if (curTimeMs > timeMs) {
                while (!queue.isEmpty()) {
                    TimeEnvelope<T> timeEnvelope = queue.poll();
                    if (timeEnvelope != null) {
                        if (timeEnvelope.isExpired() && onExpired != null) {
                            onExpired.accept(timeEnvelope.getValue());
                        }
                        itemSize.decrementAndGet();
                    }
                }
                map.remove(timeMs);
            }
        });
    }

    @Override
    public void keepAlive(AtomicBoolean isRun) {
        keepAlive(isRun, System.currentTimeMillis());
    }

}
