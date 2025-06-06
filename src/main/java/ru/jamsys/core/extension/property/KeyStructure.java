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
        if (value.startsWith(".")) {
            value = value.substring(1);
        }
        List<String> result = new ArrayList<>();
        if (value.contains("<") && value.contains(">")) {
            String[] split = value.split("<");
            for (String s : split) {
                if ("".equals(s)) {
                    continue;
                }
                if (!s.contains(">")) {
                    result.add(s);
                    continue;
                }
                String[] split1 = s.split(">");
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
    public Object getJsonValue() {
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
