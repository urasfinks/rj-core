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

    @SuppressWarnings("all")
    @PropertyNotNull
    @PropertyKey("size")
    @PropertyDescription("Размер рабочей очереди")
    private volatile Integer size = 3000;

    @SuppressWarnings("all")
    @PropertyNotNull
    @PropertyKey("tail.size")
    @PropertyDescription("Размер пробы очереди")
    private volatile Integer tailSize = 5;

    @PropertyNotNull
    @PropertyKey("retry.timeout.ms")
    @PropertyDescription("Повторный вброс, если не пришёл commit")
    private volatile Integer retryTimeoutMs = 10 * 60 * 1000; // 10 минут

    @PropertyNotNull
    @PropertyKey("fill.threshold.min")
    @PropertyDescription("Минимальный порог, когда следует наполнять очередь")
    private volatile Integer fillThresholdMin = 500;

    @PropertyNotNull
    @PropertyKey("fill.threshold.max")
    @PropertyDescription("Максимальный порог наполнения очереди helper'ом")
    private volatile Integer fillThresholdMax = 1500;

}
