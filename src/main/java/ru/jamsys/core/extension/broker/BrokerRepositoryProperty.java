package ru.jamsys.core.extension.broker;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.annotation.PropertyDescription;
import ru.jamsys.core.extension.annotation.PropertyKey;
import ru.jamsys.core.extension.annotation.PropertyNotNull;
import ru.jamsys.core.extension.property.repository.RepositoryPropertyAnnotationField;

@Getter
@FieldNameConstants
public class BrokerRepositoryProperty extends RepositoryPropertyAnnotationField<Integer> {

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

}
