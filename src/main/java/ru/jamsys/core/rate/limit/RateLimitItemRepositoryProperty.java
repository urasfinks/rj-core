package ru.jamsys.core.rate.limit;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.annotation.PropertyDescription;
import ru.jamsys.core.extension.annotation.PropertyKey;
import ru.jamsys.core.extension.property.repository.RepositoryPropertyAnnotationField;

@SuppressWarnings({"UnusedDeclaration"})
@FieldNameConstants
@Getter
public class RateLimitItemRepositoryProperty extends RepositoryPropertyAnnotationField<Integer> {

    @SuppressWarnings("all")
    @PropertyKey("max")
    @PropertyDescription("Максимальное кол-во итераций")
    private volatile Integer max = 999999;

}
