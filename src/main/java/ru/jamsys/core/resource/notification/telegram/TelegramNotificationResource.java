package ru.jamsys.core.resource.notification.telegram;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.property.PropertySubscriber;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.ResourceArguments;
import ru.jamsys.core.resource.http.client.HttpConnectorDefault;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

// TODO: морально ресурс устарел, нужен рефактор
@Component
@Scope("prototype")
public class TelegramNotificationResource
        extends ExpirationMsMutableImpl
        implements Resource<TelegramNotificationRequest, HttpResponse> {

    private SecurityComponent securityComponent;

    private PropertySubscriber propertySubscriber;

    private final TelegramNotificationProperty telegramNotificationProperty = new TelegramNotificationProperty();

    @Override
    public void setArguments(ResourceArguments resourceArguments) throws Throwable {
        securityComponent = App.get(SecurityComponent.class);
        propertySubscriber = new PropertySubscriber(
                App.get(ServiceProperty.class),
                null,
                telegramNotificationProperty,
                resourceArguments.ns
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
                telegramNotificationProperty.getUrl(),
                new String(securityComponent.get(telegramNotificationProperty.getSecurityAlias())),
                telegramNotificationProperty.getIdChat(),
                URLEncoder.encode(bodyRequest, StandardCharsets.UTF_8))
        );
        httpClient.setConnectTimeoutMs(1_000);
        httpClient.setReadTimeoutMs(telegramNotificationProperty.getTimeoutMs());
        httpClient.exec();
        return httpClient.getResponseObject();
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Function<Throwable, Boolean> getFatalException() {
        return _ -> false;
    }

    @Override
    public boolean isRun() {
        if (propertySubscriber != null) {
            return propertySubscriber.isRun();
        }
        return false;
    }

    @Override
    public void run() {
        propertySubscriber.run();
    }

    @Override
    public void shutdown() {
        propertySubscriber.shutdown();
    }

}
