package ru.jamsys.core.pool;

import lombok.Getter;
import ru.jamsys.core.extension.annotation.PropertyDescription;
import ru.jamsys.core.extension.annotation.PropertyName;
import ru.jamsys.core.extension.property.repository.AnnotationPropertyExtractor;

@Getter
public class AbstractPoolProperty extends AnnotationPropertyExtractor {

    @SuppressWarnings("all")
    @PropertyName("max")
    @PropertyDescription("Максимальное кол-во элементов в пуле")
    private volatile Integer max = 1;

    @SuppressWarnings("all")
    @PropertyName("min")
    @PropertyDescription("Минимальное кол-во элементов в пуле")
    private volatile Integer min = 0;

}
