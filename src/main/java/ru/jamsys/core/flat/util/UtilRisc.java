package ru.jamsys.core.flat.util;

import ru.jamsys.core.App;
import ru.jamsys.core.component.ExceptionHandler;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
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

    public static <T> void forEach(AtomicBoolean isRun, Collection<T> collection, Function<T, Boolean> consumer) {
        forEach(isRun, collection, consumer, false);
    }

    @SuppressWarnings("ConstantConditions")
    public static <T> void forEach(AtomicBoolean isRun, Collection<T> collection, Function<T, Boolean> consumer, boolean reverse) {
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
                    T value = objects[index];
                    if (isRun == null || isRun.get()) {
                        try {
                            if (!consumer.apply(value)) {
                                break;
                            }
                        } catch (Exception e2) {
                            App.error(e2);
                        }
                    } else {
                        break;
                    }
                    index += inc;
                    if (index < 0 || index > objects.length - 1) {
                        break;
                    }
                }
            } catch (Exception e) {
                App.error(e);
            }
        }
    }

    public static <T> void forEach(AtomicBoolean isRun, Collection<T> collection, Consumer<T> consumer) {
        forEach(isRun, collection, consumer, false);
    }

    @SuppressWarnings("ConstantConditions")
    public static <T> void forEach(AtomicBoolean isRun, Collection<T> collection, Consumer<T> consumer, boolean reverse) {
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
                    T value = objects[index];
                    if (isRun == null || isRun.get()) {
                        try {
                            consumer.accept(value);
                        } catch (Exception e2) {
                            App.error(e2);
                        }
                    } else {
                        break;
                    }
                    index += inc;
                    if (index < 0 || index > objects.length - 1) {
                        break;
                    }
                }
            } catch (Exception e) {
                App.error(e);
            }
        }
    }

    public static <K, V> void forEach(AtomicBoolean isRun, Map<K, V> map, BiFunction<K, V, Boolean> consumer) {
        if (map != null && !map.isEmpty()) {
            try {
                K[] objects = map.keySet().toArray(getEmptyType());
                for (K key : objects) {
                    if (isRun == null || isRun.get()) {
                        try {
                            V value = map.get(key);
                            if (value != null) {
                                if (!consumer.apply(key, value)) {
                                    break;
                                }
                            }
                        } catch (Exception e2) {
                            App.error(e2);
                        }
                    } else {
                        break;
                    }
                }
            } catch (Exception e) {
                App.error(e);
            }
        }
    }

    public static <K, V> void forEach(AtomicBoolean isRun, Map<K, V> map, BiConsumer<K, V> consumer) {
        if (map != null && !map.isEmpty()) {
            try {
                K[] objects = map.keySet().toArray(getEmptyType());
                for (K key : objects) {
                    if (isRun == null || isRun.get()) {
                        try {
                            V value = map.get(key);
                            if (value != null) {
                                consumer.accept(key, value);
                            }
                        } catch (Exception e2) {
                            App.error(e2);
                        }
                    } else {
                        break;
                    }
                }
            } catch (Exception e) {
                App.error(e);
            }
        }
    }

}
