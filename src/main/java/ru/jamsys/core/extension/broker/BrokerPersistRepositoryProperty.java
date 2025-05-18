package ru.jamsys.core.extension.broker;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.annotation.PropertyDescription;
import ru.jamsys.core.extension.annotation.PropertyKey;
import ru.jamsys.core.extension.annotation.PropertyNotNull;
import ru.jamsys.core.extension.property.repository.RepositoryPropertyAnnotationField;

@Getter
@FieldNameConstants
public class BrokerPersistRepositoryProperty extends RepositoryPropertyAnnotationField<Object> {

    @SuppressWarnings("all")
    @PropertyNotNull
    @PropertyKey("directory")
    @PropertyDescription("Директория для хранения .bin и .commit")
    private volatile String directory;

    @PropertyNotNull
    @PropertyKey("retry.timeout.ms")
    @PropertyDescription("Повторный вброс, если не пришёл commit")
    private volatile Integer retryTimeoutMs = 10 * 60 * 1000; // 10 минут

}
