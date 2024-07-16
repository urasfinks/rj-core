package ru.jamsys.core.component.manager.item;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.extension.property.PropertyConnector;
import ru.jamsys.core.extension.annotation.PropertyName;

@SuppressWarnings({"UnusedDeclaration"})
@Getter
public class FileByteWriterProperty extends PropertyConnector {

    @PropertyName("log.file.folder")
    private String folder = "LogManager";

    @Setter
    @PropertyName("log.file.size.kb")
    private String fileSizeKb = "20971520";

    @Setter
    @PropertyName("log.file.count")
    private String fileCount = "100";

    @Setter
    @PropertyName("log.file.name")
    private String fileName = "log";

}
