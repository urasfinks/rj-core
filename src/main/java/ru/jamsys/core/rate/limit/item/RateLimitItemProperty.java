package ru.jamsys.core.rate.limit.item;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.annotation.PropertyDescription;
import ru.jamsys.core.extension.annotation.PropertyKey;
import ru.jamsys.core.extension.property.repository.AnnotationPropertyExtractor;

@SuppressWarnings({"UnusedDeclaration"})
@FieldNameConstants
@Getter
public class RateLimitItemProperty extends AnnotationPropertyExtractor<Integer> {

    @SuppressWarnings("all")
    @PropertyKey("max")
    @PropertyDescription("Максимальное кол-во итераций")
    private volatile Integer max = 999999;

}
