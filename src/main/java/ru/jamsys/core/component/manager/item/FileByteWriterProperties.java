package ru.jamsys.core.component.manager.item;

import lombok.Getter;
import ru.jamsys.core.extension.annotation.PropertyName;
import ru.jamsys.core.extension.property.repository.RepositoryPropertiesField;

@SuppressWarnings({"UnusedDeclaration"})
@Getter
public class FileByteWriterProperties extends RepositoryPropertiesField {

    @PropertyName("log.file.folder")
    private String folder;

    @PropertyName("log.file.size.kb")
    private Integer fileSizeKb;

    @PropertyName("log.file.count")
    private Integer fileCount;

    @PropertyName("log.file.name")
    private String fileName;

}
