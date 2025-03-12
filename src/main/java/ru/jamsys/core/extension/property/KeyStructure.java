package ru.jamsys.core.extension.property;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class KeyStructure {

    final int index;

    final Map<Integer, List<Object>> map;

    public KeyStructure(int index, Map<Integer, List<Object>> map) {
        this.index = index;
        this.map = map;
    }

    public static List<String> explodeType(String value) {
        List<String> result = new ArrayList<>();
        if (value.contains("<") && value.contains(">")) {
            String[] split = value.split("<");
            for (int i = 0; i < split.length; i++) {
                if ("".equals(split[i])) {
                    continue;
                }
                if(!split[i].contains(">")){
                    result.add(split[i]);
                    continue;
                }
                String[] split1 = split[i].split(">");
                result.add("<" + split1[0] + ">");
                if (split1.length > 1) {
                    if (split1[1].startsWith(".")) {
                        result.add(split1[1].substring(1));
                    } else {
                        result.add(split1[1]);
                    }
                }
            }
        } else {
            result.add(value);
        }
        return result;
    }

    @JsonValue
    public Object get() {
        List<Object> result = new ArrayList<>();
        map.get(index).forEach(o -> {
            if (o instanceof StringBuilder) {
                result.addAll(explodeType(o.toString()));
            } else {
                result.add(o);
            }
        });
        return result;
    }

}
