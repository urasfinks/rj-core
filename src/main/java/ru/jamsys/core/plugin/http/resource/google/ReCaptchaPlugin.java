package ru.jamsys.core.plugin.http.resource.google;

import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.resource.http.client.AbstractHttpConnector;
import ru.jamsys.core.resource.http.client.HttpResponse;

import java.nio.charset.StandardCharsets;

@SuppressWarnings("unused")
public class ReCaptchaPlugin {

    public HttpResponse execute(
            AbstractHttpConnector httpClient,
            ReCaptchaRepositoryProperty repositoryProperty
    ) throws Exception {
        return httpClient
                .setBodyRaw((
                        "secret=" + new String(App.get(SecurityComponent.class).get(repositoryProperty.getSecurityAlias()))
                                + "&response=" + repositoryProperty.getCaptchaValue()
                ).getBytes(StandardCharsets.UTF_8))
                .addRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .exec();
    }

}
