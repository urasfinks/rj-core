package ru.jamsys.core.extension.broker.persist;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.annotation.PropertyDescription;
import ru.jamsys.core.extension.annotation.PropertyKey;
import ru.jamsys.core.extension.annotation.PropertyNotNull;
import ru.jamsys.core.extension.property.repository.RepositoryPropertyAnnotationField;
import ru.jamsys.core.flat.util.UtilByte;

@Getter
@FieldNameConstants
public class BrokerPersistRepositoryProperty extends RepositoryPropertyAnnotationField<Object> {

    @SuppressWarnings("all")
    @PropertyNotNull
    @PropertyKey("directory")
    @PropertyDescription("Директория для хранения .bin и .commit")
    private volatile String directory;

    @SuppressWarnings("all")
    @PropertyNotNull
    @PropertyKey("count")
    @PropertyDescription("Кол-во файлов которое храним")
    private volatile Integer count = 3;

    @SuppressWarnings("all")
    @PropertyNotNull
    @PropertyKey("retry.timeout.ms")
    @PropertyDescription("Повторный вброс, если не пришёл commit")
    private volatile Integer retryTimeoutMs = 10 * 60 * 1000; // 10 минут

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
