package ru.jamsys.core.jt;

import ru.jamsys.core.flat.template.jdbc.JdbcRequestRepository;
import ru.jamsys.core.flat.template.jdbc.StatementType;
import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;

public enum Logger implements JdbcRequestRepository {

    INSERT("""
            INSERT INTO logger (
                date_add,
                type,
                correlation,
                host,
                ext_index,
                data
            ) values (
                ${IN.date_add::TIMESTAMP},
                ${IN.type::VARCHAR},
                ${IN.correlation::VARCHAR},
                ${IN.host::VARCHAR},
                ${IN.ext_index::VARCHAR},
                ${IN.data::VARCHAR}
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
