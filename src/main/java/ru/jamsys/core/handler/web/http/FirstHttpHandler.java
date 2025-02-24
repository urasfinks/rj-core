package ru.jamsys.core.handler.web.http;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.annotation.ServiceClassFinderIgnore;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;

@ServiceClassFinderIgnore
@Component
@SuppressWarnings("unused")
@RequestMapping("/**")
public class FirstHttpHandler implements PromiseGenerator, HttpHandler {

    private final ServicePromise servicePromise;

    public FirstHttpHandler(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(getClass().getSimpleName(), 7_000L)
                .append("input", (_, _, promise) -> {
                    ServletHandler servletHandler = promise.getRepositoryMapClass(ServletHandler.class);
                    servletHandler.setResponseBody("Hello world");
                    servletHandler.setResponseHeader("opa", "cha");
                });
    }

}
