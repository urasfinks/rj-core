package ru.jamsys.core.handler.web.socket;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.flat.util.UtilLog;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;

@Component
@SuppressWarnings("unused")
@RequestMapping("/**")
public class FirstWebSocketHandler implements PromiseGenerator, WebSocketHandler {

    private final ServicePromise servicePromise;

    public FirstWebSocketHandler(ServicePromise servicePromise) {
        this.servicePromise = servicePromise;
    }

    @Override
    public Promise generate() {
        return servicePromise.get(App.getUniqueClassName(getClass()), 7_000L)
                .append("input", (_, _, promise) -> UtilLog.printInfo("Hello world"));
    }

}
