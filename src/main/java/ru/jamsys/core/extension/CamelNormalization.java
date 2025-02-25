package ru.jamsys.core.extension;

import ru.jamsys.core.flat.UtilCodeStyle;

@SuppressWarnings("unused")
public interface CamelNormalization {

    String name();

    default String getNameCamel() {
        return UtilCodeStyle.snakeToCamel(name());
    }

}
