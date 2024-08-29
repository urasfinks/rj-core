package ru.jamsys.core.web.http.plugin;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.annotation.IgnoreClassFinder;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.web.http.HttpHandler;

@IgnoreClassFinder
@Component
@SuppressWarnings("unused")
@RequestMapping({"/apple-app-site-association.json", "/.well-known/apple-app-site-association"})
public class AppleDeeplink implements PromiseGenerator, HttpHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public AppleDeeplink(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 7_000L)
                .append("input", (atomicBoolean, promise) -> {
                    ServletHandler input = promise.getRepositoryMap("HttpAsyncResponse", ServletHandler.class);
                    input.setResponseBody(Util.getWebContent(".well-known/apple-app-site-association.json"));
                });
    }

}
