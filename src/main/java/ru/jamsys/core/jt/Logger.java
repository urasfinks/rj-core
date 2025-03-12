package ru.jamsys.core.jt;

import ru.jamsys.core.flat.template.jdbc.JdbcRequestRepository;
import ru.jamsys.core.flat.template.jdbc.StatementType;
import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;

public enum Logger implements JdbcRequestRepository {

    INSERT("""
            INSERT INTO logger (
                date_add,
                type,
                host,
                data,
                header
            ) values (
                ${IN.date_add::TIMESTAMP},
                ${IN.type::VARCHAR},
                ${IN.host::VARCHAR},
                ${IN.data::VARCHAR}
                ${IN.header::VARCHAR}
            );
            """, StatementType.SELECT_WITH_AUTO_COMMIT);

    private final JdbcTemplate jdbcTemplate;

    Logger(String sql, StatementType statementType) {
        jdbcTemplate = new JdbcTemplate(sql, statementType);
    }

    @Override
    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

}
