package ru.jamsys.core.i360.scope;

import ru.jamsys.core.flat.util.FileWriteOptions;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.flat.util.UtilJson;

import java.nio.charset.StandardCharsets;

public interface ScopeJsonSerialize extends Scope {

    default void write(String path) throws Throwable {
        UtilFile.writeBytes(path, toJson().getBytes(StandardCharsets.UTF_8), FileWriteOptions.CREATE_OR_REPLACE);
    }

    default String toJson() {
        return UtilJson.toStringPretty(this, "{}");
    }

}
