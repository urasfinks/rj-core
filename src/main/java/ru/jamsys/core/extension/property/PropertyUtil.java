package ru.jamsys.core.extension.property;

import ru.jamsys.core.extension.builder.ArrayListBuilder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.flat.util.UtilJson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@SuppressWarnings("unused")
public interface PropertyUtil {

    Map<Class<?>, Function<String, ?>> convertType = new HashMap<>() {{
        this.put(String.class, s -> s);
        this.put(Integer.class, s -> Integer.parseInt(s.replace("_", "")));
        this.put(Long.class, s -> Long.parseLong(s.replace("_", "")));
        this.put(Float.class, s -> Float.parseFloat(s.replace("_", "")));
        this.put(Double.class, s -> Double.parseDouble(s.replace("_", "")));
        this.put(Short.class, Short::parseShort);
        this.put(Boolean.class, Boolean::parseBoolean);
        this.put(Map.class, string -> {
            try {
                return UtilJson.getMapOrThrow(string);
            } catch (Throwable th) {
                throw new ForwardException(th);
            }
        });
    }};

    static KeyStructure getKeyStructure(String key) {
        Map<Integer, List<Object>> x = new HashMap<>();
        x.put(0, new ArrayListBuilder<>().append(new StringBuilder()));
        int lastDepth = 0;
        int depth = 0;
        for (int i = 0; i < key.length(); i++) {
            String c = key.charAt(i) + "";
            if (c.equals("[")) {
                depth++;
                continue;
            }
            if (c.equals("]")) {
                depth--;
                continue;
            }
            if (depth != lastDepth) {
                x
                        .computeIfAbsent(depth, _ -> new ArrayList<>())
                        .add(new StringBuilder());
                if (depth > lastDepth) {
                    KeyStructure ref = new KeyStructure(depth, x);
                    x.get(lastDepth).add(ref);
                }
                lastDepth = depth;
            }
            StringBuilder last = (StringBuilder) x.get(depth).getLast();
            last.append(c);
        }
        return new KeyStructure(0, x);
    }

}
