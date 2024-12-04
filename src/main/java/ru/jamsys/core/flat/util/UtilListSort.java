package ru.jamsys.core.flat.util;

import java.util.*;
import java.util.function.Function;

public class UtilListSort {

    public enum Type {
        ASC,
        DESC
    }

    @SuppressWarnings("all")
    public static <T extends List> T sort(T list, Type type){
        T arrayList = (T) new ArrayList<>(list);
        switch (type){
            case ASC -> Collections.sort(arrayList);
            case DESC -> Collections.sort(arrayList, Collections.reverseOrder());
        }
        return arrayList;
    }

    @SuppressWarnings("all")
    public static <X extends Map, T extends List<X>> T sort(T list, Type type, Function<X, Long> fn) {
        T arrayList = (T) new ArrayList<>(list);
        Collections.sort(arrayList, (o1, o2) -> {
            Long a = fn.apply(o1);
            Long b = fn.apply(o2);
            return switch (type) {
                default -> a.compareTo(b);
                case DESC -> b.compareTo(a);
            };
        });
        return arrayList;
    }

    public static void shuffle(List<?> list){
        Collections.shuffle(list);
    }

}
