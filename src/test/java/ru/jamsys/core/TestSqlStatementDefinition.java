package ru.jamsys.core;

import lombok.Getter;
import ru.jamsys.core.flat.template.jdbc.SqlExecutionMode;
import ru.jamsys.core.flat.template.jdbc.SqlStatementDefinition;
import ru.jamsys.core.flat.template.jdbc.SqlTemplateCompiler;

@Getter
public enum TestSqlStatementDefinition implements SqlStatementDefinition {

    TEST("select * from test", SqlExecutionMode.SELECT_WITH_AUTO_COMMIT),

    GET_LOG("select * from logger", SqlExecutionMode.SELECT_WITH_AUTO_COMMIT);

    private final SqlTemplateCompiler sqlTemplateCompiler;

    private final SqlExecutionMode sqlExecutionMode;

    TestSqlStatementDefinition(String sql, SqlExecutionMode sqlExecutionMode) {
        this.sqlTemplateCompiler = new SqlTemplateCompiler(sql);
        this.sqlExecutionMode = sqlExecutionMode;
        analyze();
    }

}
