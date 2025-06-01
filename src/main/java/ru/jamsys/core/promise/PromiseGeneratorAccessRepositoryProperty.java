package ru.jamsys.core.promise;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.annotation.PropertyKey;
import ru.jamsys.core.extension.property.repository.RepositoryPropertyAnnotationField;

@SuppressWarnings({"all"})
@FieldNameConstants
@Getter
public class PromiseGeneratorAccessRepositoryProperty extends RepositoryPropertyAnnotationField<Object> {

    @PropertyKey("auth")
    private Boolean auth = false;

    @PropertyKey("users")
    private String users = "";

}
