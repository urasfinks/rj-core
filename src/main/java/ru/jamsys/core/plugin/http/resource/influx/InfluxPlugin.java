package ru.jamsys.core.plugin.http.resource.influx;

import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.resource.http.client.AbstractHttpConnector;
import ru.jamsys.core.resource.http.client.HttpResponse;

import java.nio.charset.StandardCharsets;

public class InfluxPlugin {

    public static HttpResponse execute(
            AbstractHttpConnector httpConnector,
            InfluxRepositoryProperty repositoryProperty
    ) throws Exception {
        return httpConnector
                .addRequestHeader("Content-Type", "text/plain; charset=utf-8")
                .addRequestHeader(
                        "Authorization",
                        "Token " + new String(App.get(SecurityComponent.class).get(repositoryProperty.getSecurityAlias()))
                )
                .setBodyRaw(repositoryProperty.getBodyRaw().getBytes(StandardCharsets.UTF_8))
                .exec();
    }
}
