package ru.jamsys.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ExpiredManager<T> {

    final Map<Long, ConcurrentLinkedQueue<T>> map = new ConcurrentHashMap<>();

    public void add(T obj, long timestampExpired) {
        map.putIfAbsent(timestampExpired, new ConcurrentLinkedQueue<>());
        map.get(timestampExpired).add(obj);
    }

    public int getCountBucket() {
        return map.size();
    }

    public List<T> getExpired() {
        List<T> resultList = new ArrayList<>();
        Long[] mapKeys = map.keySet().toArray(new Long[0]);
        long curTimestamp = System.currentTimeMillis();
        for (long mapKey : mapKeys) {
            if (curTimestamp >= mapKey) {
                ConcurrentLinkedQueue<T> queue = map.get(mapKey);
                if (queue != null) {
                    while (!queue.isEmpty()) {
                        resultList.add(queue.poll());
                    }
                    map.remove(mapKey);
                }
            }
        }
        return resultList;
    }
}
