package ru.jamsys.core.promise;

import ru.jamsys.core.resource.http.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class PromiseResourceFactory {

    public static PromiseTaskWithResource<HttpPoolSetting, HttpClient, HttpResponseEnvelope, HttpClientResource> getHttpTask(
            String index,
            Promise promise,
            BiConsumer<AtomicBoolean, HttpResponseEnvelope> procedure
    ) {
        return new PromiseTaskWithResource<>(
                index,
                promise,
                PromiseTaskExecuteType.IO,
                HttpClientResource.class,
                new HttpPoolSetting("wef"),
                (Promise _) -> new HttpClientImpl(),
                procedure
        );
    }

    public static PromiseTaskWithResource<HttpPoolSetting, HttpClient, HttpResponseEnvelope, HttpClientResource> getHttpTask(
            String index,
            Promise promise,
            BiFunction<AtomicBoolean, HttpResponseEnvelope, List<PromiseTask>> supplier
    ) {
        return new PromiseTaskWithResource<>(
                index,
                promise,
                PromiseTaskExecuteType.IO,
                HttpClientResource.class,
                new HttpPoolSetting("wfe"),
                (Promise _) -> new HttpClientImpl(),
                supplier
        );
    }

}
