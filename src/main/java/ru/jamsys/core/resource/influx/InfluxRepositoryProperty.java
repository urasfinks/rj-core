package ru.jamsys.core.resource.influx;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.property.repository.RepositoryPropertyAnnotationField;
import ru.jamsys.core.extension.annotation.PropertyKey;

@SuppressWarnings({"UnusedDeclaration"})
@FieldNameConstants
@Getter
public class InfluxRepositoryProperty extends RepositoryPropertyAnnotationField<String> {

    @PropertyKey("org")
    private String org;

    @PropertyKey("bucket")
    private String bucket;

    @PropertyKey("host")
    private String host;

    @PropertyKey("security.alias")
    private String alias;

}
