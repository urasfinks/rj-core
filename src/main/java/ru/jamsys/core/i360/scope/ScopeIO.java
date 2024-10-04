package ru.jamsys.core.i360.scope;

import ru.jamsys.core.flat.util.FileWriteOptions;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.flat.util.UtilFileResource;

import java.nio.charset.StandardCharsets;

// Загрузка/выгрузка модели на FS
public interface ScopeIO extends Scope {

    default void read(String path) throws Throwable {
        fromJson(UtilFileResource.getAsString(path, UtilFileResource.Direction.PROJECT));
    }

    default void write(String path) throws Throwable {
        UtilFile.writeBytes(path, toJson().getBytes(StandardCharsets.UTF_8), FileWriteOptions.CREATE_OR_REPLACE);
    }

}
