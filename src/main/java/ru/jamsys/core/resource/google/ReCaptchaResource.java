package ru.jamsys.core.resource.google;

import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.extension.expiration.AbstractExpirationResource;
import ru.jamsys.core.extension.log.DataHeader;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.resource.http.client.HttpConnectorDefault;
import ru.jamsys.core.resource.http.client.HttpResponse;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
public class ReCaptchaResource extends AbstractExpirationResource {

    private final SecurityComponent securityComponent;

    private final ReCaptchaRepositoryProperty reCaptchaRepositoryProperty = new ReCaptchaRepositoryProperty();

    private final PropertyDispatcher<String> propertyDispatcher;

    public ReCaptchaResource(String ns) {
        this.securityComponent = App.get(SecurityComponent.class);
        propertyDispatcher = new PropertyDispatcher<>(
                null,
                reCaptchaRepositoryProperty,
                getCascadeKey(ns)
        );
    }

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

    @Override
    public List<DataHeader> flushAndGetStatistic(AtomicBoolean threadRun) {
        return List.of();
    }

}
