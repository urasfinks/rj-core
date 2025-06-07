package ru.jamsys.core.resource.notification.telegram;

import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.extension.expiration.AbstractExpirationResource;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.resource.http.client.HttpConnectorDefault;
import ru.jamsys.core.resource.http.client.HttpResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class TelegramNotificationResource extends AbstractExpirationResource {

    private final SecurityComponent securityComponent;

    private final PropertyDispatcher<Object> propertyDispatcher;

    private final TelegramNotificationRepositoryProperty telegramNotificationRepositoryProperty = new TelegramNotificationRepositoryProperty();

    public TelegramNotificationResource(String ns) {
        securityComponent = App.get(SecurityComponent.class);
        propertyDispatcher = new PropertyDispatcher<>(
                null,
                telegramNotificationRepositoryProperty,
                getCascadeKey(ns)
        );
    }

    public HttpResponse execute(TelegramNotificationRequest arguments) throws Exception {
        String bodyRequest = arguments.getData();
        String title = arguments.getTitle();
        if (title != null && !title.trim().isEmpty()) {
            bodyRequest = "*" + title + "*\r\n" + bodyRequest;
        }
        HttpConnectorDefault httpClient = new HttpConnectorDefault();
        httpClient.setUrl(String.format(
                telegramNotificationRepositoryProperty.getUrl(),
                new String(securityComponent.get(telegramNotificationRepositoryProperty.getSecurityAlias())),
                telegramNotificationRepositoryProperty.getIdChat(),
                URLEncoder.encode(bodyRequest, StandardCharsets.UTF_8))
        );
        httpClient.setConnectTimeoutMs(1_000);
        httpClient.setReadTimeoutMs(telegramNotificationRepositoryProperty.getTimeoutMs());
        httpClient.exec();
        return httpClient.getHttpResponse();
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void runOperation() {
        propertyDispatcher.run();
    }

    @Override
    public void shutdownOperation() {
        propertyDispatcher.shutdown();
    }

    @Override
    public boolean checkFatalException(Throwable th) {
        return false;
    }

}
