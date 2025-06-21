package ru.jamsys.core.flat.template.jdbc;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class Argument {

    private int index;
    private Object value;

    private final ArgumentDirection direction;
    private final ArgumentType type;
    private final String key;
    private final String keySqlTemplate;

    public Argument(
            ArgumentDirection direction,
            ArgumentType type,
            String key,
            String keySqlTemplate
    ) {
        this.direction = direction;
        this.type = type;
        this.key = key;
        this.keySqlTemplate = keySqlTemplate;
    }

    public static Argument getInstance(String keyTemplate) {
        String[] keys = keyTemplate.split("\\.");
        if (keys.length == 1) {
            throw new RuntimeException("Не достаточно описания для " + keyTemplate + "; Должно быть ${direction.var::type}");
        }
        String[] types = keys[1].split("::");
        if (types.length == 1) {
            throw new RuntimeException("Не достаточно описания для " + keyTemplate + "; Должно быть ${direction.var::type}");
        }
        if (types[0].isEmpty()) {
            throw new RuntimeException("Не достаточно описания для " + keyTemplate + "; Должно быть ${direction.var::type}");
        }
        return new Argument(
                ArgumentDirection.valueOf(keys[0]),
                ArgumentType.valueOf(types[1]),
                types[0],
                keyTemplate
        );
    }

}
