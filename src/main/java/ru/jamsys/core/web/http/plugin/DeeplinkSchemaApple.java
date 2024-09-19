package ru.jamsys.core.web.http.plugin;

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
import ru.jamsys.core.web.http.HttpHandler;

/*
 * Эту драгу опрашивает Apple, что бы в телефоне зарегистрировать схему для открытия приложения
 * */
@IgnoreClassFinder
@Component
@SuppressWarnings("unused")
@RequestMapping({"/apple-app-site-association.json", "/.well-known/apple-app-site-association"})
public class DeeplinkSchemaApple implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public DeeplinkSchemaApple(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 7_000L)
                .append("input", (_, atomicBoolean, promise) -> {
                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                    servletHandler.setResponseBody(UtilFile.getWebContent(".well-known/apple-app-site-association.json"));
                });
    }

}
