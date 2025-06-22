package ru.jamsys.core;

import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.component.ServicePromise;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.template.jdbc.DataMapper;
import ru.jamsys.core.flat.template.jdbc.DebugVisualizer;
import ru.jamsys.core.flat.template.jdbc.SqlTemplateCompiled;
import ru.jamsys.core.flat.template.jdbc.SqlTemplateCompiler;
import ru.jamsys.core.flat.util.UtilLog;
import ru.jamsys.core.jt.Logger;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.resource.jdbc.JdbcResource;
import ru.jamsys.core.resource.jdbc.SqlArgumentBuilder;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// IO time: 6ms
// COMPUTE time: 6ms

class SqlTemplateCompilerRepositoryTest {

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
        SqlTemplateCompiler sqlTemplateCompiler = new SqlTemplateCompiler("""
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
                """);
        HashMap<String, Object> args = new HashMap<>();
        args.put("id_user", 1);
        args.put("uuid_device", "a1b2c3");
        args.put("lazy", List.of("Hello", "world", "wfe"));

        SqlTemplateCompiled sqlTemplateCompiled = sqlTemplateCompiler.compile(args);
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
                """, sqlTemplateCompiled.getSql(), "#1");

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
                """, DebugVisualizer.get(sqlTemplateCompiled), "#2");

    }

    @Test
    void parseSql() throws CloneNotSupportedException {
        SqlTemplateCompiler sqlTemplateCompiler = new SqlTemplateCompiler("select * from table where c1 = ${IN.name::VARCHAR} and c2 = ${IN.name::VARCHAR}");
        Assertions.assertEquals("select * from table where c1 = ? and c2 = ?", sqlTemplateCompiler.compile(new HashMap<>()).getSql(), "#1");
    }

    @Getter
    @Setter
    public static class Test1 extends DataMapper<Test1> {
        int id;
        String dataType;
    }

    @Test
    void testBridgeData() throws Throwable {
        Test1 test1 = new Test1().fromMap(new HashMapBuilder<String, Object>()
                .append("id", new BigDecimal(1))
                        .append("data_type", "hey"),
                DataMapper.TransformCodeStyle.SNAKE_TO_CAMEL
        );
        Assertions.assertEquals("hey", test1.getDataType());
        Assertions.assertEquals("{id=1, data_type=hey}", test1.toMap(DataMapper.TransformCodeStyle.CAMEL_TO_SNAKE).toString());
        Assertions.assertEquals("{id=1, dataType=hey}", test1.toMap(DataMapper.TransformCodeStyle.SNAKE_TO_CAMEL).toString());
        Assertions.assertEquals("{id=1, dataType=hey}", test1.toMap(DataMapper.TransformCodeStyle.NONE).toString());
        Assertions.assertEquals("""
                {"id":1,"dataType":"hey"}""", test1.toJson(false, DataMapper.TransformCodeStyle.NONE));
        Assertions.assertEquals("""
                {
                  "id" : 1,
                  "dataType" : "hey"
                }""", test1.toJson(true, DataMapper.TransformCodeStyle.NONE));
        Assertions.assertEquals("""
                {
                  "id" : 1,
                  "data_type" : "hey"
                }""", test1.toJson(true, DataMapper.TransformCodeStyle.CAMEL_TO_SNAKE));

        Test1 test2 = new Test1().fromJson("""
                {
                  "id" : 1,
                  "data_type" : "hey"
                }""", DataMapper.TransformCodeStyle.SNAKE_TO_CAMEL);
        Assertions.assertEquals("hey", test2.getDataType());

        Test1 test3 = new Test1().fromJson("""
                {
                  "id" : 1,
                  "dataType" : "hey"
                }""", DataMapper.TransformCodeStyle.NONE);
        Assertions.assertEquals("hey", test3.getDataType());
    }

}