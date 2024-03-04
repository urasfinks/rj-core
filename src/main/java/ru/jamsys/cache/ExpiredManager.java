package ru.jamsys.cache;

import ru.jamsys.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ExpiredManager<T> {

    final Map<Long, ConcurrentLinkedQueue<T>> map = new ConcurrentHashMap<>();

    public void add(T obj, long timeMsExpired) {
        //If the key was not present in the map, it maps the passed value to the key and returns null.
        if (!map.containsKey(timeMsExpired)) { //Что бы лишний раз не создавать ConcurrentLinkedQueue при пробе putIfAbsent
            map.putIfAbsent(timeMsExpired, new ConcurrentLinkedQueue<>());
        }
        map.get(timeMsExpired).add(obj);
    }

    public int getCountBucket() {
        return map.size();
    }

    public List<T> getExpired() {
        List<T> resultList = new ArrayList<>();
        long curMs = System.currentTimeMillis();
        Util.riskModifierMap(null, map, new Long[0], (Long timeMs, ConcurrentLinkedQueue<T> queue) -> {
            if (curMs >= timeMs) {
                while (!queue.isEmpty()) {
                    resultList.add(queue.poll());
                }
                map.remove(timeMs);
            }
        });
        return resultList;
    }
}
