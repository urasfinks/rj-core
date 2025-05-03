package ru.jamsys.core.component;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.annotation.PropertyKey;
import ru.jamsys.core.extension.property.repository.RepositoryPropertyAnnotationField;

@SuppressWarnings({"UnusedDeclaration"})
@FieldNameConstants
@Getter
public class LoggerRemoteRepositoryProperty extends RepositoryPropertyAnnotationField<Boolean> {

    @SuppressWarnings("all")
    @PropertyKey("remote")
    private Boolean remote = false;

}
