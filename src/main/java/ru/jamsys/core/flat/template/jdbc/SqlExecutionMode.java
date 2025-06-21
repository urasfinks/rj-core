package ru.jamsys.core.flat.template.jdbc;


import lombok.Getter;
import ru.jamsys.core.extension.CamelNormalization;

// Режим выполнения SQL-запроса, включая SELECT/CALL и авто-коммит.
@Getter
public enum SqlExecutionMode implements CamelNormalization {

    SELECT_WITH_AUTO_COMMIT(true, true),
    SELECT_WITHOUT_AUTO_COMMIT(false, true),
    CALL_WITH_AUTO_COMMIT(true, false),
    CALL_WITHOUT_AUTO_COMMIT(false, false);

    private final boolean autoCommit;
    private final boolean select;

    SqlExecutionMode(boolean autoCommit, boolean select) {
        this.autoCommit = autoCommit;
        this.select = select;
    }

}
