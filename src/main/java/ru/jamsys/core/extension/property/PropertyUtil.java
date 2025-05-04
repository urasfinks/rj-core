package ru.jamsys.core.extension.property;

import ru.jamsys.core.extension.builder.ArrayListBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@SuppressWarnings("unused")
public interface PropertyUtil {

    Map<Class<?>, Function<String, ?>> convertType = new HashMap<>() {{
        this.put(String.class, s -> s);
        this.put(Integer.class, Integer::parseInt);
        this.put(Long.class, Long::parseLong);
        this.put(Float.class, Float::parseFloat);
        this.put(Double.class, Double::parseDouble);
        this.put(Short.class, Short::parseShort);
        this.put(Boolean.class, Boolean::parseBoolean);
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
