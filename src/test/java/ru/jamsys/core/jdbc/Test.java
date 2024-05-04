package ru.jamsys.core.jdbc;

import ru.jamsys.core.template.jdbc.JdbcTemplate;
import ru.jamsys.core.template.jdbc.StatementType;
import ru.jamsys.core.template.jdbc.Template;

public enum Test implements JdbcTemplate {

    TEST("select * from test", StatementType.SELECT_WITH_AUTO_COMMIT);

    private final Template template;

    Test(String sql, StatementType statementType) {
        template = new Template(sql, statementType);
    }

    @Override
    public Template getTemplate() {
        return template;
    }

}
