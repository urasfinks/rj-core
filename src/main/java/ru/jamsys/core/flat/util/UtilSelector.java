package ru.jamsys.core.flat.util;

import org.springframework.lang.Nullable;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;

import java.util.List;
import java.util.Map;

public class UtilSelector {

    @Nullable
    public static <R> R selector(Map<String, Object> obj, String selector) {
        String[] path = selector.split("\\.");
        Object current = obj;
        try {
            for (int i = 0; i < path.length; i++) {
                String key = path[i];

                if (current instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) current;
                    if (!map.containsKey(key)) {
                        throw new ForwardException("Map does not contain key",
                                new HashMapBuilder<>()
                                        .append("obj", obj)
                                        .append("selector", selector)
                                        .append("mapKeys", map.keySet())
                                        .append("missingKey", key)
                        );
                    }
                    current = map.get(key);

                } else if (current instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) current;
                    int index;
                    try {
                        index = Integer.parseInt(key);
                    } catch (NumberFormatException nfe) {
                        throw new ForwardException("List key is not a valid integer index",
                                new HashMapBuilder<>()
                                        .append("obj", obj)
                                        .append("selector", selector)
                                        .append("currentListSize", list.size())
                                        .append("invalidIndex", key)
                        );
                    }
                    if (index < 0 || index >= list.size()) {
                        throw new ForwardException("Index out of bounds",
                                new HashMapBuilder<>()
                                        .append("obj", obj)
                                        .append("selector", selector)
                                        .append("currentListSize", list.size())
                                        .append("requestedIndex", index)
                        );
                    }
                    current = list.get(index);

                } else {
                    throw new ForwardException("Current object is neither Map nor List",
                            new HashMapBuilder<>()
                                    .append("obj", obj)
                                    .append("selector", selector)
                                    .append("current", current)
                                    .append("key", key)
                    );
                }

                // If last segment, return the value
                if (i == path.length - 1) {
                    @SuppressWarnings("unchecked")
                    R result = (R) current;
                    return result;
                }
            }
        } catch (Throwable th) {
            throw new ForwardException(
                    new HashMapBuilder<>()
                            .append("obj", obj)
                            .append("selector", selector),
                    th
            );
        }
        return null;
    }

}

