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
        if (keyTemplate == null || keyTemplate.isEmpty()) {
            throw new IllegalArgumentException("Пустой шаблон аргумента недопустим");
        }

        String[] keys = keyTemplate.split("\\.");
        if (keys.length < 2) {
            throwInvalidTemplate(keyTemplate);
        }

        String[] types = keys[1].split("::");
        if (types.length < 2 || types[0].isEmpty() || types[1].isEmpty()) {
            throwInvalidTemplate(keyTemplate);
        }

        return new Argument(
                ArgumentDirection.valueOf(keys[0]),
                ArgumentType.valueOf(types[1]),
                types[0],
                keyTemplate
        );
    }

    private static void throwInvalidTemplate(String keyTemplate) {
        throw new IllegalArgumentException(
                "Недостаточное описание шаблона: \"" + keyTemplate + "\". Ожидается формат: ${direction.var::type}"
        );
    }
}
