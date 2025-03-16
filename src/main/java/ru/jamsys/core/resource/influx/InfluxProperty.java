package ru.jamsys.core.resource.influx;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.property.repository.AnnotationPropertyExtractor;
import ru.jamsys.core.extension.annotation.PropertyKey;

@SuppressWarnings({"UnusedDeclaration"})
@FieldNameConstants
@Getter
public class InfluxProperty extends AnnotationPropertyExtractor<String> {

    @PropertyKey("influx.org")
    private String org;

    @PropertyKey("influx.bucket")
    private String bucket;

    @PropertyKey("influx.host")
    private String host;

    @PropertyKey("influx.security.alias")
    private String alias;

}
