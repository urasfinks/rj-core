package ru.jamsys.core.component.manager.item;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.annotation.PropertyKey;
import ru.jamsys.core.extension.annotation.PropertyNotNull;
import ru.jamsys.core.extension.property.repository.RepositoryPropertyAnnotationField;

@SuppressWarnings({"UnusedDeclaration"})
@Getter
@FieldNameConstants
public class FileByteRepositoryProperty extends RepositoryPropertyAnnotationField<Object> {

    @SuppressWarnings("all")
    @PropertyKey("folder")
    private String folder = "LogManager";

    @SuppressWarnings("all")
    @PropertyKey("file.size.kb")
    private Integer fileSizeKb = 20971520;

    @SuppressWarnings("all")
    @PropertyKey("file.count")
    private Integer fileCount = 100;

    @PropertyNotNull
    @PropertyKey("file.name")
    private String fileName;

}
