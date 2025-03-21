package ru.jamsys.core.component.manager;

import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.item.FileByteWriter;
import ru.jamsys.core.component.manager.sub.AbstractManager;
import ru.jamsys.core.extension.CascadeName;
import ru.jamsys.core.extension.KeepAliveComponent;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ManagerFileByteWriter extends AbstractManager<FileByteWriter, Void>
        implements KeepAliveComponent, CascadeName {

    public FileByteWriter get(String key, Class<?> classItem) {
        return getManagerElement(key, classItem, null);
    }

    @Override
    public FileByteWriter build(String key, Class<?> classItem, Void builderArgument) {
        return new FileByteWriter(getCascadeName(key, classItem));
    }

    @Override
    public int getInitializationIndex() {
        return 501;
    }

    @Override
    public String getKey() {
        return null;
    }

    @Override
    public CascadeName getParentCascadeName() {
        return App.cascadeName;
    }

    // Функцию пробегаться по всем писальщикам и делает централизованно запись
    // TODO: вызов этой функции надо делать в cron но никак не в keepAlive
    public void flush(AtomicBoolean threadRun) {
        getMapForFlushStatistic().forEach((_, fileByteWriter) -> fileByteWriter.flush(threadRun));
    }

}
