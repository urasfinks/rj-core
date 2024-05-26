package ru.jamsys.core.resource.jdbc;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseImpl;
import ru.jamsys.core.promise.PromiseTaskExecuteType;

import java.util.List;
import java.util.Map;

class ConnectionResourceTest {

    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        App.main(args);
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    void promiseTaskWithPool() {

        Promise promise = new PromiseImpl("testPromise", 6_000L);
        promise
                .appendWithResource("jdbc", PromiseTaskExecuteType.IO, JdbcResource.class, (_, jdbcResource) -> {
                    JdbcRequest jdbcRequest = new JdbcRequest(TestJdbcTemplate.TEST);
                    try {
                        List<Map<String, Object>> execute = jdbcResource.execute(jdbcRequest);
                        promise.setProperty("req1", execute);
                        //System.out.println(execute);
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                })
                .run()
                .await(2000);
        System.out.println(promise.getLog());

    }
}