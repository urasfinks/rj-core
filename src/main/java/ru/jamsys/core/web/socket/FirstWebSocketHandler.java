package ru.jamsys.core.web.socket;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;

@Component
@SuppressWarnings("unused")
@RequestMapping("/**")
public class FirstWebSocketHandler implements PromiseGenerator, WebSocketHandler {

    @Getter
    @Setter
    private String index;

    private final ServicePromise servicePromise;

    public FirstWebSocketHandler(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(index, 7_000L)
                .append("input", (atomicBoolean, promise) -> System.out.println("Hello world"));
    }

}
