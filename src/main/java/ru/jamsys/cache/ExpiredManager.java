package ru.jamsys.cache;

import ru.jamsys.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ExpiredManager<T> {

    final Map<Long, ConcurrentLinkedQueue<T>> map = new ConcurrentHashMap<>();

    public void add(T obj, long timestampExpired) {
        //If the key was not present in the map, it maps the passed value to the key and returns null.
        if (!map.containsKey(timestampExpired)) { //Что бы лишний раз не создавать ConcurrentLinkedQueue при пробе putIfAbsent
            map.putIfAbsent(timestampExpired, new ConcurrentLinkedQueue<>());
        }
        map.get(timestampExpired).add(obj);
    }

    public int getCountBucket() {
        return map.size();
    }

    public List<T> getExpired() {
        List<T> resultList = new ArrayList<>();
        long curTimestamp = System.currentTimeMillis();
        Util.riskModifier(map, new Long[0], (Long time, ConcurrentLinkedQueue<T> queue) -> {
            if (curTimestamp >= time) {
                while (!queue.isEmpty()) {
                    resultList.add(queue.poll());
                }
                map.remove(time);
            }
        });
        return resultList;
    }
}
