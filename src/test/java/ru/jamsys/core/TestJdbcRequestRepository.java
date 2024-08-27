package ru.jamsys.core;

import ru.jamsys.core.flat.template.jdbc.JdbcRequestRepository;
import ru.jamsys.core.flat.template.jdbc.StatementType;
import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;

public enum TestJdbcRequestRepository implements JdbcRequestRepository {

    TEST("select * from test", StatementType.SELECT_WITH_AUTO_COMMIT),

    GET_LOG("select * from logger", StatementType.SELECT_WITH_AUTO_COMMIT);


    private final JdbcTemplate jdbcTemplate;

    TestJdbcRequestRepository(String sql, StatementType statementType) {
        jdbcTemplate = new JdbcTemplate(sql, statementType);
    }

    @Override
    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

}
