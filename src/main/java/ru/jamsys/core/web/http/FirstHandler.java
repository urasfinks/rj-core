package ru.jamsys.core.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.HttpAsyncResponse;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;

@Component
@SuppressWarnings("unused")
@RequestMapping("/**")
public class FirstHandler implements PromiseGenerator {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public FirstHandler(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 7_000L)
                .append("input", (atomicBoolean, promise) -> {
                    HttpAsyncResponse input = promise.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
                    input.setBody("Hello world");
                    input.setResponseHeader("opa", "cha");
                });
    }

}
