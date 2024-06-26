package ru.jamsys.core.jt;

import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.StatementType;
import ru.jamsys.core.flat.template.jdbc.TemplateJdbc;

public enum Logger implements JdbcTemplate {

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

    private final TemplateJdbc template;

    Logger(String sql, StatementType statementType) {
        template = new TemplateJdbc(sql, statementType);
    }

    @Override
    public TemplateJdbc getTemplate() {
        return template;
    }

}
