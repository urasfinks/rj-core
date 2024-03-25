package ru.jamsys.extension;

import ru.jamsys.util.Util;

public interface EnumName {

    String name();

    default String getName() {
        return Util.snakeToCamel(name());
    }

}
