package ru.jamsys.core.component.resource;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.AbstractComponent;
import ru.jamsys.core.resource.virtual.file.system.File;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
@Lazy
public class VirtualFileSystem extends AbstractComponent<File> {

    public VirtualFileSystem() {
        setCleanableMap(false);
    }

    @Override
    public File build(String key) {
        return null;
    }

    public void add(File file) {
        put(file.getAbsolutePath(), file);
    }

}
