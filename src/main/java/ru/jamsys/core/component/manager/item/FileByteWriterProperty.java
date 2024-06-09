package ru.jamsys.core.component.manager.item;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.extension.PropertyConnector;
import ru.jamsys.core.extension.PropertyName;

@SuppressWarnings({"UnusedDeclaration"})
@Getter
public class FileByteWriterProperty extends PropertyConnector {

    @PropertyName("log.file.folder")
    String folder = "LogManager";

    @Setter
    @PropertyName("log.file.size.kb")
    String fileSizeKb = "20971520";

    @Setter
    @PropertyName("log.file.count")
    String fileCount = "100";

}
