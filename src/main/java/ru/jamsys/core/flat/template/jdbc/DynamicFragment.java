package ru.jamsys.core.flat.template.jdbc;

import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;


public abstract class DynamicFragment {

    public static final Function<Object, String> enumInFunction = object -> {
        if (object instanceof List) {
            @SuppressWarnings("rawtypes")
            List<?> listArgs = (List) object;
            if (listArgs.isEmpty()) {
                throw new RuntimeException("enumInFunction list is empty");
            }
            String[] array = new String[listArgs.size()];
            Arrays.fill(array, "?");
            return String.join(",", array);
        }
        throw new RuntimeException("enumInFunction object: " + object + " is not List");
    };

    public static final Map<ArgumentType, Function<Object, String>> dynamicType = new HashMapBuilder<ArgumentType, Function<Object, String>>()
            .append(ArgumentType.IN_ENUM_VARCHAR, enumInFunction)
            .append(ArgumentType.IN_ENUM_TIMESTAMP, enumInFunction)
            .append(ArgumentType.IN_ENUM_NUMBER, enumInFunction);

    public static final HashMapBuilder<ArgumentType, ArgumentType> mapType = new HashMapBuilder<ArgumentType, ArgumentType>()
            .append(ArgumentType.IN_ENUM_VARCHAR, ArgumentType.VARCHAR)
            .append(ArgumentType.IN_ENUM_TIMESTAMP, ArgumentType.TIMESTAMP)
            .append(ArgumentType.IN_ENUM_NUMBER, ArgumentType.NUMBER);

    public static String compile(ArgumentType argumentType, Object obj, String information) {
        // Не конкурентная проверка
        if (dynamicType.containsKey(argumentType)) {
            try {
                return dynamicType.get(argumentType).apply(obj);
            } catch (Exception e) {
                throw new ForwardException(new HashMapBuilder<>()
                        .append("object", obj)
                        .append("information", information),
                        e
                );
            }
        }
        throw new ForwardException(new HashMapBuilder<>()
                .append("cause", "Function does not exist")
                .append("argumentType", argumentType)
                .append("object", obj)
                .append("information", information)
        );
    }

    public static boolean check(ArgumentType argumentType) {
        // Не конкурентная проверка
        return dynamicType.containsKey(argumentType);
    }

}
