package ru.jamsys.core.component.resource;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.AbstractManager;
import ru.jamsys.core.resource.virtual.file.system.File;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
@Lazy
public class VirtualFileSystem extends AbstractManager<File> {

    public VirtualFileSystem() {
        setCleanableMap(false);
    }

    @Override
    public File build(String index) {
        return null;
    }

    public void add(File file) {
        put(file.getAbsolutePath(), file);
    }

}
