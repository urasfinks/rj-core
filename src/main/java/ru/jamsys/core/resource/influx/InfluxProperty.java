package ru.jamsys.core.resource.influx;

import lombok.Getter;
import ru.jamsys.core.extension.property.repository.AnnotationPropertyExtractor;
import ru.jamsys.core.extension.annotation.PropertyName;

@SuppressWarnings({"UnusedDeclaration"})
@Getter
public class InfluxProperty extends AnnotationPropertyExtractor {

    @PropertyName("influx.org")
    private String org;

    @PropertyName("influx.bucket")
    private String bucket;

    @PropertyName("influx.host")
    private String host;

    @PropertyName("influx.security.alias")
    private String alias;

}
