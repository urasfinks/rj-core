package ru.jamsys.core.plugin.promise;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.handler.web.http.HttpHandler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGeneratorExternalRequest;
import ru.jamsys.core.promise.PromiseGeneratorExternalRequestRepositoryProperty;

import java.nio.charset.StandardCharsets;

/*
 * Зарегистрированные Property
 * */

@Component
@SuppressWarnings("unused")
@RequestMapping("/property")
public class PropertyWeb extends PromiseGeneratorExternalRequest implements HttpHandler {

    private final ServicePromise servicePromise;

    public PropertyWeb(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
        getPropertyDispatcher().set(PromiseGeneratorExternalRequestRepositoryProperty.Fields.auth, true);
    }

    @Override
    public Promise generate() {
        return servicePromise.get(App.getUniqueClassName(getClass()), 7_000L)
                .append("input", (_, _, promise) -> {
                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                    servletHandler.getResponseHeader().append("Content-Type", "application/json");
                    servletHandler.send(App.get(ServiceProperty.class).getJsonValue(), StandardCharsets.UTF_8);
                });
    }

}
