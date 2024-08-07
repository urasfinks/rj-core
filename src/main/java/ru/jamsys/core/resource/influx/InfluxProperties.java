package ru.jamsys.core.resource.influx;

import lombok.Getter;
import ru.jamsys.core.extension.property.repository.RepositoryPropertiesField;
import ru.jamsys.core.extension.annotation.PropertyName;

@SuppressWarnings({"UnusedDeclaration"})
@Getter
public class InfluxProperties extends RepositoryPropertiesField {

    @PropertyName("influx.org")
    private String org;

    @PropertyName("influx.bucket")
    private String bucket;

    @PropertyName("influx.host")
    private String host;

    @PropertyName("influx.security.alias")
    private String alias;

}
