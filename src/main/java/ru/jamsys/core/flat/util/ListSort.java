package ru.jamsys.core.flat.util;

import java.util.*;

public class ListSort<T> {

    Map<Integer, T> map = new HashMap<>();

    public void add(Integer index, T object) {
        map.put(index, object);
    }

    public List<T> getSorted() {
        List<T> result = new ArrayList<>();
        SortedSet<Integer> keys = new TreeSet<>(map.keySet());
        for (Integer key : keys) {
            result.add(map.get(key));
        }
        return result;
    }
}
