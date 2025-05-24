package ru.jamsys.core.resource.google;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.http.client.HttpConnectorDefault;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImplAbstractLifeCycle;

import java.nio.charset.StandardCharsets;

@SuppressWarnings("unused")
@Component
@Scope("prototype")
public class ReCaptchaResource
        extends ExpirationMsMutableImplAbstractLifeCycle
        implements Resource<String, HttpResponse>, CascadeKey {

    private final SecurityComponent securityComponent;

    private final ReCaptchaRepositoryProperty reCaptchaRepositoryProperty = new ReCaptchaRepositoryProperty();

    private PropertyDispatcher<String> propertyDispatcher;

    public ReCaptchaResource(SecurityComponent securityComponent) {
        this.securityComponent = securityComponent;
    }

    @Override
    public void init(String ns) throws Throwable {
        propertyDispatcher = new PropertyDispatcher<>(
                null,
                reCaptchaRepositoryProperty,
                getCascadeKey(ns)
        );
    }

    @Override
    public HttpResponse execute(String captchaValue) {
        HttpConnectorDefault httpClient = new HttpConnectorDefault();
        httpClient.setUrl("https://www.google.com/recaptcha/api/siteverify");

        String body = "secret=" + new String(securityComponent.get(reCaptchaRepositoryProperty.getSecurityAlias())) + "&response=" + captchaValue;
        httpClient.setPostData(body.getBytes(StandardCharsets.UTF_8));

        httpClient.setConnectTimeoutMs(1_000);
        httpClient.setReadTimeoutMs(3_000);
        httpClient.setRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
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
