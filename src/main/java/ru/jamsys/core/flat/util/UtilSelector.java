package ru.jamsys.core.flat.util;

import org.springframework.lang.Nullable;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;

import java.util.Map;

public class UtilSelector {

    @Nullable
    public static <R> R selector(Map<String, Object> obj, String selector) {
        String[] path = selector.split("\\.");
        Object current = obj;
        try {
            for (int i = 0; i < path.length; i++) {
                if (!(current instanceof Map)) {
                    throw new ForwardException("Is not a Map", new HashMapBuilder<>()
                            .append("obj", obj)
                            .append("selector", selector)
                            .append("current", current)
                            .append("key", i > 0 ? path[i - 1] : "(root)")
                    );
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) current;
                if (!map.containsKey(path[i])) {
                    throw new ForwardException("Map not contains key", new HashMapBuilder<>()
                            .append("obj", obj)
                            .append("selector", selector)
                            .append("mapKeys", map.keySet())
                            .append("key", path[i])
                    );
                }
                current = map.get(path[i]);
                if (i == path.length - 1) {
                    @SuppressWarnings("unchecked")
                    R o = (R) current;
                    return o;
                }
            }
        } catch (Throwable th) {
            throw new ForwardException(new HashMapBuilder<>()
                    .append("obj", obj)
                    .append("selector", selector), th);
        }
        return null;
    }

}
