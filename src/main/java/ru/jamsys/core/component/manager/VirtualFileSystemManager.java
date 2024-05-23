package ru.jamsys.core.component.manager;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.resource.virtual.file.system.File;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
@Lazy
public class VirtualFileSystemManager extends AbstractManager<File> {

    public VirtualFileSystemManager() {
        setCleanableMap(false);
    }

    public void add(File file) {
        map.put(file.getAbsolutePath(), file);
    }

    // setCleanableMap(false) - можем себе позволить прихранивать объекты
    public File get(String key) {
        return map.get(key);
    }

    @Override
    public File build(String index, Class<?> classItem) {
        return null;
    }

}
