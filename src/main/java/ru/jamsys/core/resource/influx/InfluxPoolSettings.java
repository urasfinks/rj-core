package ru.jamsys.core.resource.influx;

import ru.jamsys.core.component.manager.sub.PoolResourceArgument;

public class InfluxPoolSettings {

    // пространство из *.properties
    public final String namespaceProperties;

    public static PoolResourceArgument<
            InfluxClientResource,
            InfluxPoolSettings
            > setting = new PoolResourceArgument<>(InfluxClientResource.class, new InfluxPoolSettings(), _ -> false);

    public InfluxPoolSettings(String namespaceProperties) {
        this.namespaceProperties = namespaceProperties;
    }

    public InfluxPoolSettings() {
        this.namespaceProperties = "default";
    }
}
