package ru.jamsys.core.web.http;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;

@Component
@SuppressWarnings("unused")
@RequestMapping("/test/*")
public class SecondHandler implements PromiseGenerator {

    @Getter
    @Setter
    String index;

    private final ServicePromise servicePromise;

    public SecondHandler(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 1_000L)
                .append("input", (atomicBoolean, promise) -> {
                    Util.sleepMs(3000);
//                    HttpAsyncResponse input = promise.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
//                    input.setBody("Hello world");
//                    input.setResponseHeader("opa", "cha");
                });
    }

}
