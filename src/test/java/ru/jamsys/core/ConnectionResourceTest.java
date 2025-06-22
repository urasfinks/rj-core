package ru.jamsys.core;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.flat.template.jdbc.ArgumentType;
import ru.jamsys.core.jt.Logger;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.core.resource.jdbc.SqlArgumentBuilder;

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
    // @Test
    void promiseTaskWithPool() {
        Promise promise = servicePromise.get("testPromise", 6_000L);
        promise
                .appendWithResource("jdbc", JdbcResource.class, "postgresql.remote.log", (_, _, _, jdbcResource) -> {
                    List<Map<String, Object>> execute = jdbcResource.execute(
                            Logger.INSERT_LOG,
                            new SqlArgumentBuilder()
                                    .add("uuid", java.util.UUID.randomUUID().toString())
                                    .add("message", "Hello world")
                                    .add("date_add", System.currentTimeMillis())
                                    .add("tag_keys", jdbcResource.createArray(
                                            ArgumentType.VARCHAR,
                                            List.of("t1", "t2").toArray())
                                    )
                                    .add("tag_values", jdbcResource.createArray(
                                            ArgumentType.VARCHAR,
                                            List.of("v1", "v2").toArray())
                                    )
                    );
                })
                .run()
                .await(2000);

    }

    @SuppressWarnings("all")
    void testInsertLog() {
        Promise promise = servicePromise.get("testPromisePosgreSQL", 6_000L);
        promise
                .appendWithResource("jdbc", JdbcResource.class, "logger", (_, _, _, jdbcResource) -> {
                    try {
                        SqlArgumentBuilder sqlArgumentBuilder = new SqlArgumentBuilder();
                        int idx = 0;
                        for (int i = 0; i < 2; i++) {
                            sqlArgumentBuilder
                                    .add("date_add", System.currentTimeMillis())
                                    .add("type", "Info")
                                    .add("correlation", java.util.UUID.randomUUID().toString())
                                    .add("host", "abc")
                                    .add("ext_index", null)
                                    .add("data", "{\"idx\":" + (idx++) + "}")
                                    .nextBatch()
                            ;
                        }
                        //List<Map<String, Object>> execute = jdbcResource.execute(Logger.INSERT, sqlArgumentBuilder);
                    } catch (Throwable th) {
                        App.error(th);
                    }
                })
                .run()
                .await(2000);
    }

}