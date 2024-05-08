package ru.jamsys.core.util;

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

    public static <T> void forEach(AtomicBoolean isRun, Collection<T> collection, Function<T, Boolean> consumer, boolean reverse) {
        T[] toArray = getEmptyType();
        if (collection != null && !collection.isEmpty()) {
            try {
                T[] objects = collection.toArray(toArray);
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
                            App.context.getBean(ExceptionHandler.class).handler(e2);
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
                App.context.getBean(ExceptionHandler.class).handler(e);
            }
        }
    }

    public static <T> void forEach(AtomicBoolean isRun, Collection<T> collection, Consumer<T> consumer) {
        forEach(isRun, collection, consumer, false);
    }

    public static <T> void forEach(AtomicBoolean isRun, Collection<T> collection, Consumer<T> consumer, boolean reverse) {
        T[] toArray = getEmptyType();
        if (collection != null && !collection.isEmpty()) {
            try {
                T[] objects = collection.toArray(toArray);
                int index = reverse ? objects.length - 1 : 0;
                int inc = reverse ? -1 : 1;
                while (true) {
                    T value = objects[index];
                    if (isRun == null || isRun.get()) {
                        try {
                            consumer.accept(value);
                        } catch (Exception e2) {
                            App.context.getBean(ExceptionHandler.class).handler(e2);
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
                App.context.getBean(ExceptionHandler.class).handler(e);
            }
        }
    }

    public static <K, V> void forEach(AtomicBoolean isRun, Map<K, V> map, BiFunction<K, V, Boolean> consumer) {
        K[] toArray = getEmptyType();
        if (map != null && !map.isEmpty()) {
            try {
                K[] objects = map.keySet().toArray(toArray);
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
                            App.context.getBean(ExceptionHandler.class).handler(e2);
                        }
                    } else {
                        break;
                    }
                }
            } catch (Exception e) {
                App.context.getBean(ExceptionHandler.class).handler(e);
            }
        }
    }

    public static <K, V> void forEach(AtomicBoolean isRun, Map<K, V> map, BiConsumer<K, V> consumer) {
        K[] toArray = getEmptyType();
        if (map != null && !map.isEmpty()) {
            try {
                K[] objects = map.keySet().toArray(toArray);
                for (K key : objects) {
                    if (isRun == null || isRun.get()) {
                        try {
                            V value = map.get(key);
                            if (value != null) {
                                consumer.accept(key, value);
                            }
                        } catch (Exception e2) {
                            App.context.getBean(ExceptionHandler.class).handler(e2);
                        }
                    } else {
                        break;
                    }
                }
            } catch (Exception e) {
                App.context.getBean(ExceptionHandler.class).handler(e);
            }
        }
    }

}
