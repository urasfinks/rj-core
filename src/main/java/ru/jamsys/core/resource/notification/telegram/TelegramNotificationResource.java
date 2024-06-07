package ru.jamsys.core.resource.notification.telegram;

import lombok.Setter;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.PropComponent;
import ru.jamsys.core.component.SecurityComponent;
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

    @Setter
    private String securityAlias;

    @Setter
    private String url;

    @Setter
    private String idChat;

    @Setter
    private int timeoutMs;

    private SecurityComponent securityComponent;

    @Override
    public void constructor(NamespaceResourceConstructor constructor) throws Throwable {
        PropComponent propComponent = App.context.getBean(PropComponent.class);
        securityComponent = App.context.getBean(SecurityComponent.class);

        propComponent.getProp(constructor.ns, "notification.telegram.url", s -> this.url = s);
        propComponent.getProp(constructor.ns, "notification.telegram.idChat", s -> this.idChat = s);
        propComponent.getProp(constructor.ns, "notification.telegram.security.alias", s -> this.securityAlias = s);
        propComponent.getProp(constructor.ns, "notification.telegram.timeoutMs", integer -> this.timeoutMs = Integer.parseInt(integer));
    }

    @Override
    public HttpResponse execute(TelegramNotificationRequest arguments) {
        String bodyRequest = arguments.getData();
        String title = arguments.getTitle();
        if (title != null && !title.trim().isEmpty()) {
            bodyRequest = "*" + title + "*\r\n" + bodyRequest;
        }
        HttpClientImpl httpClient = new HttpClientImpl();
        httpClient.setUrl(String.format(url, new String(securityComponent.get(securityAlias)), idChat, URLEncoder.encode(bodyRequest, StandardCharsets.UTF_8)));
        httpClient.setTimeoutMs(timeoutMs);
        httpClient.exec();
        return httpClient.getHttpResponseEnvelope();
    }

    @Override
    public void close() {

    }

    @Override
    public int getWeight(BalancerAlgorithm balancerAlgorithm) {
        return 0;
    }

}
