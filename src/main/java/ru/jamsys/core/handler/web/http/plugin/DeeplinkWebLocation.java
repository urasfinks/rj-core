package ru.jamsys.core.handler.web.http.plugin;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.annotation.IgnoreClassFinder;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.flat.template.twix.TemplateTwix;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.handler.web.http.HttpHandler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;

import java.util.Map;

/*
 * Посадочная web страница, при открытии которой будет попытка открыть приложение по зарегистрированной схеме
 * */
@IgnoreClassFinder
@Component
@SuppressWarnings("unused")
@RequestMapping("/deeplink/**")
public class DeeplinkWebLocation implements PromiseGenerator, HttpHandler {

    private final ServicePromise servicePromise;

    public DeeplinkWebLocation(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(getClass().getSimpleName(), 7_000L)
                .append("input", (_, _, promise) -> {
                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                    servletHandler.setResponseContentType("text/html");
                    Map<String, String> args = new HashMapBuilder<String, String>()
                            .append("urlSchemes", App.get(ServiceProperty.class)
                                    .get(
                                            "run.args.plugin.deeplink.url.schemes",
                                            "",
                                            getClass().getName()
                                    )
                                    .get()
                            )
                            .append("urlIosAppStore", App.get(ServiceProperty.class)
                                    .get(
                                            "run.args.plugin.deeplink.url.ios.app.store",
                                            "",
                                            getClass().getName()
                                    )
                                    .get()
                            );
                    servletHandler.setResponseBody(TemplateTwix.template(
                            UtilFileResource.getAsString(
                                    "static/deeplink.html",
                                    UtilFileResource.Direction.valueOf(
                                            App.get(ServiceProperty.class)
                                                    .get(
                                                            "run.args.plugin.deeplink.template.class.loader",
                                                            "",
                                                            getClass().getName()
                                                    )
                                                    .get()
                                    )
                            ),
                            args
                    ));
                });
    }

}
