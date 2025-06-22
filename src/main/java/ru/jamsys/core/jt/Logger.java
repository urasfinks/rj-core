package ru.jamsys.core.jt;

import lombok.Getter;
import ru.jamsys.core.flat.template.jdbc.SqlExecutionMode;
import ru.jamsys.core.flat.template.jdbc.SqlStatementDefinition;
import ru.jamsys.core.flat.template.jdbc.SqlTemplateCompiler;

@Getter
public enum Logger implements SqlStatementDefinition {

    DROP_OLD_PARTITION("""
            CALL drop_old_partitions(
                ${IN.days_threshold::NUMBER}::integer
            );""", SqlExecutionMode.CALL_WITH_AUTO_COMMIT),

    CREATE_PARTITIONS("""
            CALL create_partitions(
                now()::timestamp,
                ${IN.days::NUMBER}::integer
            );""", SqlExecutionMode.CALL_WITH_AUTO_COMMIT),

    INSERT_LOG("""
            CALL insert_log_with_tags(
                ${IN.uuid::VARCHAR}::uuid,
                ${IN.message::VARCHAR},
                ${IN.date_add::TIMESTAMP},
                ${IN.tag_keys::VARCHAR}::text[],
                ${IN.tag_values::VARCHAR}::text[]
            );
            """, SqlExecutionMode.CALL_WITH_AUTO_COMMIT);

    private final SqlTemplateCompiler sqlTemplateCompiler;

    private final SqlExecutionMode sqlExecutionMode;

    Logger(String sql, SqlExecutionMode sqlExecutionMode) {
        this.sqlTemplateCompiler = new SqlTemplateCompiler(sql);
        this.sqlExecutionMode = sqlExecutionMode;
        analyze();
    }

}
