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

    @SuppressWarnings("all")
    public static <T extends List<?>> T sortAsc(T list){
        T arrayList = (T) new ArrayList<>(list);
        Collections.sort((List) arrayList);
        return arrayList;
    }

    @SuppressWarnings("all")
    public static <T extends List<?>> T sortDesc(T list){
        T arrayList = (T) new ArrayList<>(list);
        Collections.sort((List) arrayList, Collections.reverseOrder());
        return arrayList;
    }

}
