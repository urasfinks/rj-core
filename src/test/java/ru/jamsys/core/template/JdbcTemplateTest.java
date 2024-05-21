package ru.jamsys.core.template;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.flat.template.jdbc.CompiledSqlTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;
import ru.jamsys.core.flat.template.jdbc.Template;

import java.util.HashMap;
import java.util.List;

class JdbcTemplateTest {

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
    void parseSql2() throws Exception {
        Template template = new Template("""
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

        CompiledSqlTemplate compiledSqlTemplate = template.compile(args);
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
                """, template.debug(compiledSqlTemplate), "#2");
    }

    @Test
    void parseSql() {
        Template template = new Template("select * from table where c1 = ${IN.name::VARCHAR} and c2 = ${IN.name::VARCHAR}", StatementType.SELECT_WITH_AUTO_COMMIT);
        Assertions.assertEquals("select * from table where c1 = ? and c2 = ?", template.getSql(), "#1");
    }
}