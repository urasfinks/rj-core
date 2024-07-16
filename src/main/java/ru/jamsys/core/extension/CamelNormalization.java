package ru.jamsys.core.extension;

import ru.jamsys.core.flat.util.Util;

@SuppressWarnings("unused")
public interface CamelNormalization {

    String name();

    default String getNameCamel() {
        return Util.snakeToCamel(name());
    }

}
