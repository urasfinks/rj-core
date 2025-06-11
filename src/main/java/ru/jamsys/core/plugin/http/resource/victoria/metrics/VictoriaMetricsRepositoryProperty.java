package ru.jamsys.core.plugin.http.resource.victoria.metrics;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.annotation.PropertyKey;
import ru.jamsys.core.extension.annotation.PropertyNotNull;
import ru.jamsys.core.extension.property.repository.RepositoryPropertyAnnotationField;

@SuppressWarnings({"UnusedDeclaration", "all"})
@Getter
@FieldNameConstants
public class VictoriaMetricsRepositoryProperty extends RepositoryPropertyAnnotationField<String> {

    @PropertyNotNull
    @PropertyKey("body.raw")
    private String bodyRaw;

}
