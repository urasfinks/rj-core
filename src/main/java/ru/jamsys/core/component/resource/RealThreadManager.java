package ru.jamsys.core.component.resource;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.promise.PromiseTask;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
@Lazy
public class RealThreadManager {

    public void submit(PromiseTask task) {

    }

}