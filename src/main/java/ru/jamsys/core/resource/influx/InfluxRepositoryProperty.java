package ru.jamsys.core.resource.influx;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.property.repository.RepositoryPropertyAnnotationField;
import ru.jamsys.core.extension.annotation.PropertyKey;

@SuppressWarnings({"UnusedDeclaration"})
@FieldNameConstants
@Getter
public class InfluxRepositoryProperty extends RepositoryPropertyAnnotationField<String> {

    @PropertyKey("influx.org")
    private String org;

    @PropertyKey("influx.bucket")
    private String bucket;

    @PropertyKey("influx.host")
    private String host;

    @PropertyKey("influx.security.alias")
    private String alias;

}
