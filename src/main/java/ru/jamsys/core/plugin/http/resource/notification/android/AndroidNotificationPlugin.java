package ru.jamsys.core.plugin.http.resource.notification.android;

import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.resource.http.client.AbstractHttpConnector;
import ru.jamsys.core.resource.http.client.HttpResponse;

import java.nio.charset.StandardCharsets;

public class AndroidNotificationPlugin {

    public static HttpResponse execute(
            AbstractHttpConnector abstractHttpConnector,
            AndroidNotificationRepositoryProperty repositoryProperty
    ) {
        ManagerConfiguration<GoogleCredentials> googleCredentials = ManagerConfiguration.getInstance(
                repositoryProperty.getApplicationName(),
                App.getUniqueClassName(GoogleCredentials.class),
                GoogleCredentials.class,
                null
        );
        abstractHttpConnector
                .setBodyRaw(createBodyRaw(repositoryProperty).getBytes(StandardCharsets.UTF_8))
                .addRequestHeader("Content-type", "application/json")
                .addRequestHeader("Authorization", "Bearer " + googleCredentials
                        .get()
                        .getAccessToken()
                )
        ;
        return abstractHttpConnector.exec();
    }

    private static String createBodyRaw(AndroidNotificationRepositoryProperty repositoryProperty) {
        return UtilJson.toStringPretty(new HashMapBuilder<String, Object>()
                .append("message", new HashMapBuilder<String, Object>()
                        .append("token", repositoryProperty.getToken())
                        .append("notification", new HashMapBuilder<String, Object>()
                                .append("title", repositoryProperty.getApplicationName())
                                .append("body", repositoryProperty.getTitle())
                        )
                        .apply(map -> {
                            if (repositoryProperty.getData() != null) {
                                map.append("data", new HashMapBuilder<String, Object>()
                                        .append("message", UtilJson.toString(repositoryProperty.getData(), "{}")));
                            }
                        })
                ), "{}");
    }

}
