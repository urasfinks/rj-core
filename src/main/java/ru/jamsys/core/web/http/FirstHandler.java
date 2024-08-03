package ru.jamsys.core.web.http;

import lombok.Setter;
import ru.jamsys.core.HttpAsyncResponse;
import ru.jamsys.core.extension.annotation.PromiseWeb;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.promise.PromiseImpl;

@Setter
@SuppressWarnings("unused")
@PromiseWeb("/test")
public class FirstHandler implements PromiseGenerator {

    private String index;

    @Override
    public Promise generate() {
        return new PromiseImpl(index, 7_000)
                .append("input", (atomicBoolean, promise) -> {
                    HttpAsyncResponse input = promise.getRepositoryMap("input", HttpAsyncResponse.class);
                });
    }

}
