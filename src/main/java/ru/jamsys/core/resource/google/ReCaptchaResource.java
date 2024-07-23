package ru.jamsys.core.resource.google;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.property.NameSpaceAgent;
import ru.jamsys.core.resource.Resource;
import ru.jamsys.core.resource.ResourceArguments;
import ru.jamsys.core.resource.http.client.HttpClientImpl;
import ru.jamsys.core.resource.http.client.HttpResponse;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

@Component
@Scope("prototype")
public class ReCaptchaResource
        extends ExpirationMsMutableImpl
        implements Resource<String, HttpResponse> {

    private final SecurityComponent securityComponent;

    private final ReCaptchaProperty property = new ReCaptchaProperty();

    private NameSpaceAgent nameSpaceAgent;

    public ReCaptchaResource(SecurityComponent securityComponent) {
        this.securityComponent = securityComponent;
    }

    @Override
    public void setArguments(ResourceArguments resourceArguments) throws Throwable {
        ServiceProperty serviceProperty = App.get(ServiceProperty.class);
        nameSpaceAgent = serviceProperty.getSubscriber(null, property, resourceArguments.ns);
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
    public Function<Throwable, Boolean> getFatalException() {
        return _ -> false;
    }

    @Override
    public void run() {
        if (nameSpaceAgent != null) {
            nameSpaceAgent.run();
        }
    }

    @Override
    public void shutdown() {
        if (nameSpaceAgent != null) {
            nameSpaceAgent.shutdown();
        }
    }

}
