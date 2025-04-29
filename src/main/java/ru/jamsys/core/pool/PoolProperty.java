package ru.jamsys.core.pool;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.annotation.PropertyDescription;
import ru.jamsys.core.extension.annotation.PropertyKey;
import ru.jamsys.core.extension.property.repository.AnnotationPropertyExtractor;

@FieldNameConstants
@Getter
public class PoolProperty extends AnnotationPropertyExtractor<Integer> {

    @SuppressWarnings("all")
    @PropertyKey("max")
    @PropertyDescription("Максимальное кол-во элементов в пуле")
    private volatile Integer max = 1;

    @SuppressWarnings("all")
    @PropertyKey("min")
    @PropertyDescription("Минимальное кол-во элементов в пуле")
    private volatile Integer min = 0;

}
