package ru.jamsys.core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;

import java.util.List;
import java.util.Map;

class ConnectionResourceTest {

    static public ServicePromise servicePromise;

    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runCore();
        servicePromise = App.get(ServicePromise.class);
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @SuppressWarnings("unused")
        // Без контекста БД невозможно в тестах запускать
        //@Test
    void promiseTaskWithPool() {

        Promise promise = servicePromise.get("testPromise", 6_000L);
        promise
                .appendWithResource("jdbc", JdbcResource.class, (_, _, _, jdbcResource) -> {
                    JdbcRequest jdbcRequest = new JdbcRequest(TestJdbcRequestRepository.TEST);
                    try {
                        List<Map<String, Object>> execute = jdbcResource.execute(jdbcRequest);
                        promise.setRepositoryMap("req1", execute);
                    } catch (Throwable e) {
                        throw new ForwardException(e);
                    }
                })
                .run()
                .await(2000);

    }
}