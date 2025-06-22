package ru.jamsys.core.flat.template.jdbc;

import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;


public abstract class DynamicFragment {

    private static final Function<Object, String> enumInFunction = obj -> {
        List<?> values = castToList(obj);

        if (values.isEmpty()) {
            throw new IllegalArgumentException("DynamicFragment: IN list is empty");
        }

        String[] placeholders = new String[values.size()];
        Arrays.fill(placeholders, "?");
        return String.join(",", placeholders);
    };

    private static final Map<ArgumentType, Function<Object, String>> dynamicType = Map.of(
            ArgumentType.IN_ENUM_VARCHAR, enumInFunction,
            ArgumentType.IN_ENUM_TIMESTAMP, enumInFunction,
            ArgumentType.IN_ENUM_NUMBER, enumInFunction
    );

    public static final Map<ArgumentType, ArgumentType> mapType = Map.of(
            ArgumentType.IN_ENUM_VARCHAR, ArgumentType.VARCHAR,
            ArgumentType.IN_ENUM_TIMESTAMP, ArgumentType.TIMESTAMP,
            ArgumentType.IN_ENUM_NUMBER, ArgumentType.NUMBER
    );

    public static String compile(ArgumentType argumentType, Object obj){
        Function<Object, String> compiler = dynamicType.get(argumentType);

        if (compiler == null) {
            throw new ForwardException("Unsupported dynamic argument type", new HashMapBuilder<String, Object>()
                    .append("argumentType", argumentType)
                    .append("object", obj)
            );
        }

        try {
            return compiler.apply(obj);
        } catch (Exception e) {
            throw new ForwardException(new HashMapBuilder<String, Object>()
                    .append("object", obj)
                    .append("argumentType", argumentType)
                    , e);
        }
    }

    public static boolean check(ArgumentType argumentType) {
        return dynamicType.containsKey(argumentType);
    }

    private static List<?> castToList(Object obj) {
        if (obj instanceof List<?>) {
            return (List<?>) obj;
        }
        throw new IllegalArgumentException("DynamicFragment: " + "enumInFunction" + " expects List, got " +
                (obj == null ? "null" : obj.getClass().getSimpleName()));
    }

}
