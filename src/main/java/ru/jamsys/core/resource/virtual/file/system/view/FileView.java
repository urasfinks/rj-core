package ru.jamsys.core.resource.virtual.file.system.view;

import ru.jamsys.core.resource.virtual.file.system.File;

public interface FileView {

    // Это вместо конструктора, тут не надо создавать кеш, оставьте это для createCache
    void set(File file);

    // Создать кеш
    void createCache();

}
