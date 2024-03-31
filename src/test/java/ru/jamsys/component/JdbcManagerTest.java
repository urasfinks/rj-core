package ru.jamsys.component;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.App;
import ru.jamsys.thread.task.JdbcRequest;

class JdbcManagerTest {

    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        App.main(args);
    }

    @Test
    void get() {
        JdbcRequest jdbcRequest = new JdbcRequest("f1", ru.jamsys.component.jdbc.Test.TEST, 1_000);
        JdbcManager jdbcManager = App.context.getBean(JdbcManager.class);
        //List<Map<String, Object>> result = jdbcManager.execTask(jdbcRequest);
        //System.out.println(result);
    }

}