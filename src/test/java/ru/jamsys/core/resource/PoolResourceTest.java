package ru.jamsys.core.resource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;

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

//        PoolResource<HttpClient, HttpResponseEnvelope, HttpClientResource> pool = new PoolResource<>("testPoolResource", 0, HttpClientResource.class);
//        pool.run();
//
//        Promise promise = new PromiseImpl("testPromise", 6_000L);
//
//        promise
//                .append(new PromiseTaskWithResource<>(
//                        "testTaskWithResource",
//                        promise,
//                        PromiseTaskExecuteType.IO,
//                        pool,
//                        new HttpClientImpl(),
//                        (AtomicBoolean isRun, HttpResponseEnvelope httpResponseEnvelope) -> {
//                            System.out.println("!!!");
//                        }
//                ))
//                .run()
//                .await(1000);
//
//        System.out.println(promise.getLog());

    }
}