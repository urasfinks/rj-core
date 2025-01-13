package ru.jamsys.core;

import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.template.jdbc.JdbcDataImportExport;
import ru.jamsys.core.flat.template.jdbc.CompiledSqlTemplate;
import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;
import ru.jamsys.core.jt.Logger;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.resource.jdbc.JdbcRequest;
import ru.jamsys.core.resource.jdbc.JdbcResource;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// IO time: 6ms
// COMPUTE time: 6ms

class JdbcRequestRepositoryTest {

    public static ServicePromise servicePromise;

    @BeforeAll
    static void beforeAll() {
        App.getRunBuilder().addTestArguments().runCore();
        servicePromise = App.get(ServicePromise.class);
    }

    @AfterAll
    static void shutdown() {
        App.shutdown();
    }

    @Test
    void parseSql2() throws Exception {
        JdbcTemplate jdbcTemplate = new JdbcTemplate("""
                SELECT
                    type_data AS key,
                    max(revision_data) AS max
                FROM data WHERE (type_data IN ('%s', '%s', '%s', '%s', '%s', '%s') AND id_user = 1 )
                OR ( type_data IN ('%s', '%s') AND id_user = ${IN.id_user::NUMBER})
                OR ( type_data = '%s' AND (uuid_device_data = ${IN.uuid_device::VARCHAR} OR id_user = ${IN.id_user::NUMBER}))
                AND lazy_sync_data IN (${IN.lazy::IN_ENUM_VARCHAR})
                AND id_user = ${IN.id_user::NUMBER})
                AND lazy_sync_data IN (${IN.lazy::IN_ENUM_VARCHAR})
                AND uuid_device_data = ${IN.uuid_device::VARCHAR}
                GROUP BY type_data;
                """, StatementType.SELECT_WITH_AUTO_COMMIT);
        HashMap<String, Object> args = new HashMap<>();
        args.put("id_user", 1);
        args.put("uuid_device", "a1b2c3");
        args.put("lazy", List.of("Hello", "world", "wfe"));

        CompiledSqlTemplate compiledSqlTemplate = jdbcTemplate.compile(args);
        Assertions.assertEquals("""
                SELECT
                    type_data AS key,
                    max(revision_data) AS max
                FROM data WHERE (type_data IN ('%s', '%s', '%s', '%s', '%s', '%s') AND id_user = 1 )
                OR ( type_data IN ('%s', '%s') AND id_user = ?)
                OR ( type_data = '%s' AND (uuid_device_data = ? OR id_user = ?))
                AND lazy_sync_data IN (?,?,?)
                AND id_user = ?)
                AND lazy_sync_data IN (?,?,?)
                AND uuid_device_data = ?
                GROUP BY type_data;
                """, compiledSqlTemplate.getSql(), "#1");

        Assertions.assertEquals("""
                SELECT
                    type_data AS key,
                    max(revision_data) AS max
                FROM data WHERE (type_data IN ('%s', '%s', '%s', '%s', '%s', '%s') AND id_user = 1 )
                OR ( type_data IN ('%s', '%s') AND id_user = 1)
                OR ( type_data = '%s' AND (uuid_device_data = 'a1b2c3' OR id_user = 1))
                AND lazy_sync_data IN ('Hello','world','wfe')
                AND id_user = 1)
                AND lazy_sync_data IN ('Hello','world','wfe')
                AND uuid_device_data = 'a1b2c3'
                GROUP BY type_data;
                """, jdbcTemplate.getSqlWithArgumentsValue(compiledSqlTemplate), "#2");
    }

    @Test
    void parseSql() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate("select * from table where c1 = ${IN.name::VARCHAR} and c2 = ${IN.name::VARCHAR}", StatementType.SELECT_WITH_AUTO_COMMIT);
        Assertions.assertEquals("select * from table where c1 = ? and c2 = ?", jdbcTemplate.getSql(), "#1");
    }

    void testPostgreSql() {
        Promise promise = servicePromise.get("testPromisePosgreSQL", 6_000L);
        promise
                .appendWithResource("jdbc", JdbcResource.class, "logger", (_, _, _, jdbcResource) -> {
                    try {
                        List<Map<String, Object>> execute = jdbcResource.execute(new JdbcRequest(TestJdbcRequestRepository.GET_LOG));
                        System.out.println(execute);
                    } catch (Throwable th) {
                        App.error(th);
                    }
                })
                .run()
                .await(2000);
        //System.out.println(promise.getLog());
    }

    void testInsertLog() {
        Promise promise = servicePromise.get("testPromisePosgreSQL", 6_000L);
        promise
                .appendWithResource("jdbc", JdbcResource.class, "logger", (_, _, _, jdbcResource) -> {
                    try {
                        JdbcRequest jdbcRequest = new JdbcRequest(Logger.INSERT);
                        int idx = 0;
                        for (int i = 0; i < 2; i++) {
                            jdbcRequest
                                    .addArg("date_add", System.currentTimeMillis())
                                    .addArg("type", "Info")
                                    .addArg("correlation", java.util.UUID.randomUUID().toString())
                                    .addArg("host", "abc")
                                    .addArg("ext_index", null)
                                    .addArg("data", "{\"idx\":" + (idx++) + "}")
                                    .nextBatch()
                            ;
                        }
                        List<Map<String, Object>> execute = jdbcResource.execute(jdbcRequest);
                        System.out.println(execute);
                    } catch (Throwable th) {
                        App.error(th);
                    }
                })
                .run()
                .await(2000);
        //System.out.println(promise.getLog());
    }

    @Getter
    @Setter
    public static class Test1 extends JdbcDataImportExport<Test1> {
        int id;
        String dataType;
    }

    @Test
    void testBridgeData() {
        Test1 test1 = new Test1().fromMap(new HashMapBuilder<String, Object>()
                .append("id", new BigDecimal(1))
                .append("data_type", "hey")
        );
        Assertions.assertEquals("hey", test1.getDataType());
        Assertions.assertEquals("{id=1, data_type=hey}", test1.toMap().toString());
    }

}