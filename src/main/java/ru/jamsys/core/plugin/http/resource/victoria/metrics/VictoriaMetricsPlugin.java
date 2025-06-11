package ru.jamsys.core.plugin.http.resource.victoria.metrics;

import ru.jamsys.core.resource.http.client.AbstractHttpConnector;
import ru.jamsys.core.resource.http.client.HttpResponse;

import java.nio.charset.StandardCharsets;

public class VictoriaMetricsPlugin {

    public static HttpResponse execute(
            AbstractHttpConnector httpConnector,
            VictoriaMetricsRepositoryProperty repositoryProperty
    ) {
        return httpConnector
                .addRequestHeader("Content-Type", "text/plain")
                .setBodyRaw(repositoryProperty.getBodyRaw().getBytes(StandardCharsets.UTF_8))
                .exec();
    }
}
