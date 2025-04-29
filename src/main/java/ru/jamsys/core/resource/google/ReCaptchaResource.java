package ru.jamsys.core.resource.google;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.ResourceConfiguration;
import ru.jamsys.core.resource.http.client.HttpConnectorDefault;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImplAbstractLifeCycle;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

@Component
@Scope("prototype")
public class ReCaptchaResource
        extends ExpirationMsMutableImplAbstractLifeCycle
        implements Resource<String, HttpResponse> {

    private final SecurityComponent securityComponent;

    private final ReCaptchaProperty reCaptchaProperty = new ReCaptchaProperty();

    private PropertyDispatcher<String> propertyDispatcher;

    public ReCaptchaResource(SecurityComponent securityComponent) {
        this.securityComponent = securityComponent;
    }

    @Override
    public void init(ResourceConfiguration resourceConfiguration) throws Throwable {
        propertyDispatcher = new PropertyDispatcher<>(
                App.get(ServiceProperty.class),
                null,
                reCaptchaProperty,
                resourceConfiguration.ns
        );
    }

    @Override
    public HttpResponse execute(String captchaValue) {
        HttpConnectorDefault httpClient = new HttpConnectorDefault();
        httpClient.setUrl("https://www.google.com/recaptcha/api/siteverify");

        String body = "secret=" + new String(securityComponent.get(reCaptchaProperty.getSecurityAlias())) + "&response=" + captchaValue;
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
    public Function<Throwable, Boolean> getFatalException() {
        return _ -> false;
    }

    @Override
    public void runOperation() {
        propertyDispatcher.run();
    }

    @Override
    public void shutdownOperation() {
        propertyDispatcher.shutdown();
    }

}
