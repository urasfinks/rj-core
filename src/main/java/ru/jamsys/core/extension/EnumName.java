package ru.jamsys.core.extension;

import ru.jamsys.core.flat.util.Util;

@SuppressWarnings("unused")
public interface EnumName {

    String name();

    default String getName() {
        return Util.snakeToCamel(name());
    }

}
