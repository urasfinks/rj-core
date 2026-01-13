package ru.jamsys.core.flat.template.scheduler;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.annotation.PropertyDescription;
import ru.jamsys.core.extension.annotation.PropertyKey;
import ru.jamsys.core.extension.annotation.PropertyNotNull;
import ru.jamsys.core.extension.property.repository.RepositoryPropertyAnnotationField;

@SuppressWarnings({"UnusedDeclaration", "all"})
@FieldNameConstants
@Getter
public class SchedulerRepositoryProperty extends RepositoryPropertyAnnotationField<Object> {

    @PropertyKey("minIntervalMillis")
    @PropertyDescription("Имя telegram бота")
    @PropertyNotNull
    private Integer minIntervalMillis = 1000;

}
