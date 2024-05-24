package ru.jamsys.core.resource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseImpl;
import ru.jamsys.core.promise.PromiseTaskExecuteType;
import ru.jamsys.core.promise.PromiseTaskWithResource;
import ru.jamsys.core.resource.http.HttpClientImpl;
import ru.jamsys.core.resource.http.HttpClientResource;
import ru.jamsys.core.resource.http.HttpResponseEnvelope;

import java.util.concurrent.atomic.AtomicBoolean;

class PoolResourceTest {
    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        App.main(args);
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @Test
    void test() {
        Promise promise = new PromiseImpl("testPromise", 6_000L);
        promise
                .append(new PromiseTaskWithResource<>(
                        "testTaskWithResource",
                        promise,
                        PromiseTaskExecuteType.IO,
                        HttpClientResource.class,
                        (Promise _) -> new HttpClientImpl(),
                        (AtomicBoolean _, HttpResponseEnvelope _) -> System.out.println("!!!")
                ))
                .run()
                .await(1000);
        System.out.println(promise.getLog());
    }
}