package ru.jamsys.core.handler.web.http.plugin;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.annotation.IgnoreClassFinder;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.handler.web.http.HttpHandler;

/*
 * Эту драгу опрашивает Google, что бы в телефоне зарегистрировать схему для открытия приложения
 * */
@IgnoreClassFinder
@Component
@SuppressWarnings("unused")
@RequestMapping("/.well-known/assetlinks.json")
public class DeeplinkSchemaAndroid implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public DeeplinkSchemaAndroid(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 7_000L)
                .append("input", (_, _, promise) -> {
                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                    servletHandler.setResponseBody(UtilFile.getWebContent(".well-known/assetlinks.json"));
                });
    }

}