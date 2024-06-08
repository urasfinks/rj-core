package ru.jamsys.core.resource.google;

import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.PropertyComponent;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.extension.Subscriber;
import ru.jamsys.core.resource.NamespaceResourceConstructor;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.balancer.algorithm.BalancerAlgorithm;
import ru.jamsys.core.resource.http.client.HttpClientImpl;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.nio.charset.StandardCharsets;

@Component
public class ReCaptchaResource
        extends ExpirationMsMutableImpl
        implements Resource<NamespaceResourceConstructor, String, HttpResponse> {

    private final SecurityComponent securityComponent;

    private final ReCaptchaProperty property = new ReCaptchaProperty();

    private Subscriber subscriber;

    public ReCaptchaResource(SecurityComponent securityComponent) {
        this.securityComponent = securityComponent;
    }

    @Override
    public void constructor(NamespaceResourceConstructor constructor) throws Throwable {
        PropertyComponent propertyComponent = App.context.getBean(PropertyComponent.class);
        subscriber = propertyComponent.getSubscriber(null, property, constructor.ns);
    }

    @Override
    public HttpResponse execute(String captchaValue) {
        HttpClientImpl httpClient = new HttpClientImpl();
        httpClient.setUrl("https://www.google.com/recaptcha/api/siteverify");

        String body = "secret=" + new String(securityComponent.get(property.getSecurityAlias())) + "&response=" + captchaValue;
        httpClient.setPostData(body.getBytes(StandardCharsets.UTF_8));

        httpClient.setTimeoutMs(3000);
        httpClient.setRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
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
