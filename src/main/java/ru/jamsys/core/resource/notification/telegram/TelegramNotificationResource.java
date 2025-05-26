package ru.jamsys.core.resource.notification.telegram;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.http.client.HttpConnectorDefault;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.extension.expiration.mutable.ExpirationMsMutableImplAbstractLifeCycle;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

// TODO: морально ресурс устарел, нужен рефактор
@Component
@Scope("prototype")
public class TelegramNotificationResource
        extends ExpirationMsMutableImplAbstractLifeCycle
        implements Resource<TelegramNotificationRequest, HttpResponse>, CascadeKey {

    private SecurityComponent securityComponent;

    private PropertyDispatcher<Object> propertyDispatcher;

    private final TelegramNotificationRepositoryProperty telegramNotificationRepositoryProperty = new TelegramNotificationRepositoryProperty();

    @Override
    public void init(String ns) throws Throwable {
        securityComponent = App.get(SecurityComponent.class);
        propertyDispatcher = new PropertyDispatcher<>(
                null,
                telegramNotificationRepositoryProperty,
                getCascadeKey(ns)
        );
    }

    @Override
    public HttpResponse execute(TelegramNotificationRequest arguments) {
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
        return httpClient.getResponseObject();
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
