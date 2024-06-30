package ru.jamsys.core.component.manager;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.sub.AbstractManager;
import ru.jamsys.core.extension.KeepAliveComponent;
import ru.jamsys.core.resource.virtual.file.system.File;

@Component
@Lazy
public class ManagerVirtualFileSystem extends AbstractManager<File, Void> implements KeepAliveComponent {

    public ManagerVirtualFileSystem() {
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
    public File build(String index, Class<?> classItem, Void builderArgument) {
        return null;
    }

    @Override
    public int getInitializationIndex() {
        return 502;
    }

}
