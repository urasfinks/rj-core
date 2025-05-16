package ru.jamsys.core.extension.batch.writer;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.annotation.PropertyDescription;
import ru.jamsys.core.extension.annotation.PropertyKey;
import ru.jamsys.core.extension.property.repository.RepositoryPropertyAnnotationField;
import ru.jamsys.core.flat.util.UtilByte;

@Getter
@FieldNameConstants
public class AsyncFileWriterRepositoryProperty extends RepositoryPropertyAnnotationField<Object> {

    // Надо уточнить, файлы создаются в разрезе секунды (планировщик), если за секунду удасться записать больше данных
    // они будут записаны, но в следующую секунду будет создан новый файл
    @SuppressWarnings("all")
    @PropertyKey("max.size")
    @PropertyDescription("Максимальный размер файла в байтах")
    private volatile Long maxSize = UtilByte.megabytesToBytes(20);

    @SuppressWarnings("all")
    @PropertyKey("flush.max.time.ms")
    @PropertyDescription("Максимальное время работы flush")
    private volatile Integer flushMaxTimeMs = 950;

    @SuppressWarnings("all")
    @PropertyKey("flush.each.time.ms")
    @PropertyDescription("Время между сбросами на файловую систему, если очень большая очередь")
    private volatile Integer flushEachTimeMs = 50;

}
