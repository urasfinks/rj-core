package ru.jamsys.core.component.manager;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
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
        if (file == null) {
            App.error(new RuntimeException("file is null"));
            return;
        }
        externalPut(file.getAbsolutePath(), file);
    }

    // setCleanableMap(false) - можем себе позволить прихранивать объекты
    public File get(String key) {
        return externalGet(key);
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
