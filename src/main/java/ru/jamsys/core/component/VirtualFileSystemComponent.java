package ru.jamsys.core.component;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.AbstractManager;
import ru.jamsys.core.resource.virtual.file.system.File;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
@Lazy
public class VirtualFileSystemComponent extends AbstractManager<File> {

    public VirtualFileSystemComponent() {
        setCleanableMap(false);
    }

    @Override
    public File build(String index) {
        return null;
    }

    public void add(File file) {
        put(file.getAbsolutePath(), file);
    }

    // setCleanableMap(false) - можем себе позволить прихранивать объекты
    @Override
    public File get(String key) {
        return super.get(key);
    }
}
