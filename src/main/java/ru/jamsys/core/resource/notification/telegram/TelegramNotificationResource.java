package ru.jamsys.core.resource.notification.telegram;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.property.PropertyNsAgent;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.ResourceArguments;
import ru.jamsys.core.resource.http.client.HttpClientImpl;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

@Component
@Scope("prototype")
public class TelegramNotificationResource
        extends ExpirationMsMutableImpl
        implements Resource<TelegramNotificationRequest, HttpResponse> {

    private SecurityComponent securityComponent;

    private PropertyNsAgent propertyNsAgent;

    private final TelegramNotificationProperty property = new TelegramNotificationProperty();

    @Override
    public void setArguments(ResourceArguments resourceArguments) throws Throwable {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        securityComponent = App.get(SecurityComponent.class);
        propertyNsAgent = serviceProperty.getPropertyNsAgent(null, property, resourceArguments.ns);
    }

    @Override
    public HttpResponse execute(TelegramNotificationRequest arguments) {
        String bodyRequest = arguments.getData();
        String title = arguments.getTitle();
        if (title != null && !title.trim().isEmpty()) {
            bodyRequest = "*" + title + "*\r\n" + bodyRequest;
        }
        HttpClientImpl httpClient = new HttpClientImpl();
        httpClient.setUrl(String.format(
                property.getUrl(),
                new String(securityComponent.get(property.getSecurityAlias())),
                property.getIdChat(),
                URLEncoder.encode(bodyRequest, StandardCharsets.UTF_8))
        );
        httpClient.setTimeoutMs(Integer.parseInt(property.getTimeoutMs()));
        httpClient.exec();
        return httpClient.getHttpResponseEnvelope();
    }

    @Override
    public Function<Throwable, Boolean> getFatalException() {
        return _ -> false;
    }

    @Override
    public void run() {
        if (propertyNsAgent != null) {
            propertyNsAgent.run();
        }
    }

    @Override
    public void shutdown() {
        if (propertyNsAgent != null) {
            propertyNsAgent.shutdown();
        }
    }

}
