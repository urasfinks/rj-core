package ru.jamsys.core.resource.influx;

import lombok.Getter;
import ru.jamsys.core.extension.PropertyConnector;
import ru.jamsys.core.extension.PropertyName;

@SuppressWarnings({"UnusedDeclaration"})
@Getter
public class InfluxProperty extends PropertyConnector {

    @PropertyName("influx.org")
    private String org;

    @PropertyName("influx.bucket")
    private String bucket;

    @PropertyName("influx.host")
    private String host;

    @PropertyName("influx.security.alias")
    private String alias;

}
