package ru.jamsys.core.plugin.promise;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.annotation.ServiceClassFinderIgnore;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.handler.web.http.HttpHandler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGeneratorExternalRequest;

/*
 * Эту драгу опрашивает Google, что бы в телефоне зарегистрировать схему для открытия приложения
 * */
@ServiceClassFinderIgnore
@Component
@SuppressWarnings("unused")
@RequestMapping("/.well-known/assetlinks.json")
public class DeeplinkSchemaAndroid extends PromiseGeneratorExternalRequest implements HttpHandler {

    private final ServicePromise servicePromise;

    public DeeplinkSchemaAndroid(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(App.getUniqueClassName(getClass()), 7_000L)
                .append("input", (_, _, promise) -> {
                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                    servletHandler.setResponseBody(UtilFile.getWebFileAsString(".well-known/assetlinks.json"));
                });
    }

}
