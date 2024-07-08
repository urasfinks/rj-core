package ru.jamsys.core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.ForwardException;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;

import java.util.List;
import java.util.Map;

class ConnectionResourceTest {

    static public ServicePromise servicePromise;

    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{
                "run.args.remote.log=false",
                "run.args.remote.statistic=false"
        };
        App.run(args);
        servicePromise = App.get(ServicePromise.class);
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    void promiseTaskWithPool() {

        Promise promise = servicePromise.get("testPromise", 6_000L);
        promise
                .appendWithResource("jdbc", JdbcResource.class, (_, _, jdbcResource) -> {
                    JdbcRequest jdbcRequest = new JdbcRequest(TestJdbcTemplate.TEST);
                    try {
                        List<Map<String, Object>> execute = jdbcResource.execute(jdbcRequest);
                        promise.setProperty("req1", execute);
                        //System.out.println(execute);
                    } catch (Throwable e) {
                        throw new ForwardException(e);
                    }
                })
                .run()
                .await(2000);
        System.out.println(promise.getLogString());

    }
}