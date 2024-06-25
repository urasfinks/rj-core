package ru.jamsys.core.resource.jdbc;

import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;
import ru.jamsys.core.flat.template.jdbc.TemplateJdbc;

public enum TestJdbcTemplate implements JdbcTemplate {

    TEST("select * from test", StatementType.SELECT_WITH_AUTO_COMMIT),
    GET_LOG("select * from logger", StatementType.SELECT_WITH_AUTO_COMMIT);

    private final TemplateJdbc template;

    TestJdbcTemplate(String sql, StatementType statementType) {
        template = new TemplateJdbc(sql, statementType);
    }

    @Override
    public TemplateJdbc getTemplate() {
        return template;
    }

}
