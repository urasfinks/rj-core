package ru.jamsys.virtual.file.system.view;

import ru.jamsys.virtual.file.system.File;

public interface FileView {
    void set(File file); //Это вместо кнструктора, тут не надо создавать кеш, оставте это для createCache
    void createCache(); //Что-то произошло с данными, надо пересобрать кеш
}
