package ru.jamsys.core.handler.web.http.plugin;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.handler.web.http.HttpHandler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGeneratorAccess;
import ru.jamsys.core.promise.PromiseGeneratorAccessRepositoryProperty;

/*
 * Зарегистрированные Property
 * */

@Component
@SuppressWarnings("unused")
@RequestMapping("/property")
public class PropertyWeb extends PromiseGeneratorAccess implements HttpHandler {

    private final ServicePromise servicePromise;

    public PropertyWeb(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
        getPropertyDispatcher().set(PromiseGeneratorAccessRepositoryProperty.Fields.auth, true);
        getPropertyDispatcher().set(PromiseGeneratorAccessRepositoryProperty.Fields.users, "admin");
    }

    @Override
    public Promise generate() {
        return servicePromise.get(App.getUniqueClassName(getClass()), 7_000L)
                .append("input", (_, _, promise) -> {
                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                    servletHandler.setResponseContentType("application/json");
                    servletHandler.setResponseBody(App.get(ServiceProperty.class).getJsonValue());
                });
    }

}
