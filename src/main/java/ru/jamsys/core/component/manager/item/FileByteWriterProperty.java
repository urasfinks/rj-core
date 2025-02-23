package ru.jamsys.core.component.manager.item;

import lombok.Getter;
import ru.jamsys.core.extension.annotation.PropertyName;
import ru.jamsys.core.extension.annotation.PropertyNotNull;
import ru.jamsys.core.extension.property.repository.AnnotationPropertyExtractor;

@SuppressWarnings({"UnusedDeclaration"})
@Getter
public class FileByteWriterProperty extends AnnotationPropertyExtractor {

    @SuppressWarnings("all")
    @PropertyName("folder")
    private String folder = "LogManager";

    @SuppressWarnings("all")
    @PropertyName("file.size.kb")
    private Integer fileSizeKb = 20971520;

    @SuppressWarnings("all")
    @PropertyName("file.count")
    private Integer fileCount = 100;

    @PropertyNotNull
    @PropertyName("file.name")
    private String fileName;

}
