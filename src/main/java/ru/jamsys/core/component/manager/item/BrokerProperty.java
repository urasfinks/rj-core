package ru.jamsys.core.component.manager.item;

import lombok.Getter;
import ru.jamsys.core.extension.annotation.PropertyDescription;
import ru.jamsys.core.extension.annotation.PropertyName;
import ru.jamsys.core.extension.annotation.PropertyNotNull;
import ru.jamsys.core.extension.property.repository.AnnotationPropertyExtractor;

@Getter
public class BrokerProperty extends AnnotationPropertyExtractor {

    @SuppressWarnings("all")
    @PropertyNotNull
    @PropertyName("size")
    @PropertyDescription("Размер рабочей очереди")
    private volatile Integer size = 3000;

    @SuppressWarnings("all")
    @PropertyNotNull
    @PropertyName("tail.size")
    @PropertyDescription("Размер пробы очереди")
    private volatile Integer tailSize = 5;

}
