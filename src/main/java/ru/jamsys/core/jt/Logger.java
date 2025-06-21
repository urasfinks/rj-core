package ru.jamsys.core.jt;

import lombok.Getter;
import ru.jamsys.core.flat.template.jdbc.SqlExecutionMode;
import ru.jamsys.core.flat.template.jdbc.SqlStatementDefinition;
import ru.jamsys.core.flat.template.jdbc.SqlTemplateCompiler;

@Getter
public enum Logger implements SqlStatementDefinition {

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
            """, SqlExecutionMode.SELECT_WITH_AUTO_COMMIT);

    private final SqlTemplateCompiler sqlTemplateCompiler;

    private final SqlExecutionMode sqlExecutionMode;

    Logger(String sql, SqlExecutionMode sqlExecutionMode) {
        this.sqlTemplateCompiler = new SqlTemplateCompiler(sql);
        this.sqlExecutionMode = sqlExecutionMode;
        analyze();
    }

}
