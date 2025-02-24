package ru.jamsys.core.pool;

import lombok.Getter;
import ru.jamsys.core.extension.annotation.PropertyName;
import ru.jamsys.core.extension.property.repository.AnnotationPropertyExtractor;

@Getter
public class AbstractPoolProperty extends AnnotationPropertyExtractor {

    @SuppressWarnings("all")
    @PropertyName("max")
    private volatile Integer max = 1;

    @SuppressWarnings("all")
    @PropertyName("min")
    private volatile Integer min = 0;

}
