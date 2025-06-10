package ru.jamsys.core.plugin.http.resource;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.resource.http.client.AbstractHttpConnector;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.resource.notification.telegram.TelegramNotificationRepositoryProperty;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Getter
@Setter
public class TelegramNotificationPlugin {

    public static HttpResponse execute(
            AbstractHttpConnector abstractHttpConnector,
            TelegramNotificationRepositoryProperty repositoryProperty
    ) throws Exception {
        abstractHttpConnector.setUrl(String.format(
                abstractHttpConnector.getUrl(),
                new String(App.get(SecurityComponent.class).get(repositoryProperty.getSecurityAlias())),
                repositoryProperty.getIdChat(),
                URLEncoder.encode(repositoryProperty.getMessage(), StandardCharsets.UTF_8)
        ));
        return abstractHttpConnector.exec();
    }

}
