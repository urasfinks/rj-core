package ru.jamsys.core.component.manager.item;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.annotation.PropertyDescription;
import ru.jamsys.core.extension.annotation.PropertyKey;
import ru.jamsys.core.extension.annotation.PropertyNotNull;
import ru.jamsys.core.extension.property.repository.AnnotationPropertyExtractor;

@Getter
@FieldNameConstants
public class BrokerProperty extends AnnotationPropertyExtractor<Integer> {

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
