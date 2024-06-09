package ru.jamsys.core.resource.notification.telegram;

import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.PropertyComponent;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.extension.property.Subscriber;
import ru.jamsys.core.resource.NamespaceResourceConstructor;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.balancer.algorithm.BalancerAlgorithm;
import ru.jamsys.core.resource.http.client.HttpClientImpl;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class TelegramNotificationResource
        extends ExpirationMsMutableImpl
        implements Resource<NamespaceResourceConstructor, TelegramNotificationRequest, HttpResponse> {

    private SecurityComponent securityComponent;

    private Subscriber subscriber;

    private final TelegramNotificationProperty property = new TelegramNotificationProperty();

    @Override
    public void constructor(NamespaceResourceConstructor constructor) throws Throwable {
        PropertyComponent propertyComponent = App.context.getBean(PropertyComponent.class);
        securityComponent = App.context.getBean(SecurityComponent.class);
        subscriber = propertyComponent.getSubscriber(null, property, constructor.ns);
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
    public void close() {
        subscriber.unsubscribe();
    }

    @Override
    public int getWeight(BalancerAlgorithm balancerAlgorithm) {
        return 0;
    }

}
