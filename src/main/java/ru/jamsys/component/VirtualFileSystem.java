package ru.jamsys.component;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.component.general.AbstractComponent;
import ru.jamsys.virtual.file.system.File;

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
        addItem(file.getAbsolutePath(), file);
    }

}
