package ru.jamsys.core.flat.util;

import ru.jamsys.core.extension.exception.ForwardException;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class UtilRisc {

    @SafeVarargs
    public static <ANY> ANY[] getEmptyType(ANY... array) {
        return Arrays.copyOf(array, 0);
    }

    public static <T> void forEach(AtomicBoolean run, Collection<T> collection, Function<T, Boolean> consumer) {
        forEach(run, collection, consumer, false);
    }

    @SuppressWarnings("ConstantConditions")
    public static <T> void forEach(AtomicBoolean run, Collection<T> collection, Function<T, Boolean> consumer, boolean reverse) {
        if (collection != null && !collection.isEmpty()) {
            try {
                T[] objects = collection.toArray(getEmptyType());
                // Наткнулся на неатомарность: Index 0 out of bounds for length 0
                if (objects.length == 0) {
                    return;
                }
                int index = reverse ? objects.length - 1 : 0;
                int inc = reverse ? -1 : 1;
                while (true) {
                    if (run == null || run.get()) {
                        if (!consumer.apply(objects[index])) {
                            break;
                        }
                    } else {
                        break;
                    }
                    index += inc;
                    if (index < 0 || index > objects.length - 1) {
                        break;
                    }
                }
            } catch (Throwable th) {
                throw new ForwardException(th);
            }
        }
    }

    public static <T> void forEach(AtomicBoolean run, Collection<T> collection, Consumer<T> consumer) {
        forEach(run, collection, consumer, false);
    }

    @SuppressWarnings("ConstantConditions")
    public static <T> void forEach(AtomicBoolean run, Collection<T> collection, Consumer<T> consumer, boolean reverse) {
        if (collection != null && !collection.isEmpty()) {
            try {
                T[] objects = collection.toArray(getEmptyType());
                // Наткнулся на неатомарность: Index 0 out of bounds for length 0
                if (objects.length == 0) {
                    return;
                }
                int index = reverse ? objects.length - 1 : 0;
                int inc = reverse ? -1 : 1;
                while (true) {
                    if (run == null || run.get()) {
                        consumer.accept(objects[index]);
                    } else {
                        break;
                    }
                    index += inc;
                    if (index < 0 || index > objects.length - 1) {
                        break;
                    }
                }
            } catch (Throwable th) {
                throw new ForwardException(th);
            }
        }
    }

    public static <K, V> void forEach(AtomicBoolean run, Map<K, V> map, BiFunction<K, V, Boolean> consumer) {
        if (map != null && !map.isEmpty()) {
            try {
                K[] objects = map.keySet().toArray(getEmptyType());
                for (K key : objects) {
                    if (run == null || run.get()) {
                        // Так как мы бежим по копии ключей, то есть вероятность, что ключ уже удалили
                        // поэтому каждый раз надо проверять
                        if (!map.containsKey(key)) {
                            continue;
                        }
                        if (!consumer.apply(key, map.get(key))) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
            } catch (Throwable th) {
                throw new ForwardException(th);
            }
        }
    }

    public static <K, V> void forEach(AtomicBoolean run, Map<K, V> map, BiConsumer<K, V> consumer) {
        if (map != null && !map.isEmpty()) {
            try {
                K[] objects = map.keySet().toArray(getEmptyType());
                for (K key : objects) {
                    if (run == null || run.get()) {
                        // Так как мы бежим по копии ключей, то есть вероятность, что ключ уже удалили
                        // поэтому каждый раз надо проверять
                        if (!map.containsKey(key)) {
                            continue;
                        }
                        consumer.accept(key, map.get(key));
                    } else {
                        break;
                    }
                }
            } catch (Throwable th) {
                throw new ForwardException(th);
            }
        }
    }

    public static <T, R> List<R> forEach(T[] array, Function<T, R> fn) {
        List<R> list = new ArrayList<>();
        for (T item : array) {
            R r = fn.apply(item);
            if (r != null) {
                list.add(r);
            }
        }
        return list;
    }

}
