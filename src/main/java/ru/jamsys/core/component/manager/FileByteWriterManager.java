package ru.jamsys.core.component.manager;

import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.item.FileByteWriter;
import ru.jamsys.core.component.manager.sub.AbstractManager;
import ru.jamsys.core.extension.KeepAliveComponent;

@Component
public class FileByteWriterManager extends AbstractManager<FileByteWriter, Void> implements KeepAliveComponent {

    public FileByteWriter get(String index) {
        return getManagerElement(index, Void.class, null);
    }

    @Override
    public FileByteWriter build(String index, Class<?> classItem, Void builderArgument) {
        return new FileByteWriter(index);
    }

    @Override
    public int getInitializationIndex() {
        return 999;
    }

}
