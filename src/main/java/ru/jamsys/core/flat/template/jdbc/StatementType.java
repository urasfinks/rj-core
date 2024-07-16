package ru.jamsys.core.flat.template.jdbc;


import ru.jamsys.core.extension.CamelNormalization;

public enum StatementType implements CamelNormalization {

    SELECT_WITH_AUTO_COMMIT,
    SELECT_WITHOUT_AUTO_COMMIT,
    CALL_WITH_AUTO_COMMIT,
    CALL_WITHOUT_AUTO_COMMIT;

    public boolean isAutoCommit() {
        return this == StatementType.CALL_WITH_AUTO_COMMIT || this == StatementType.SELECT_WITH_AUTO_COMMIT;
    }

    public boolean isSelect() {
        return this == StatementType.SELECT_WITH_AUTO_COMMIT || this == StatementType.SELECT_WITHOUT_AUTO_COMMIT;
    }

    @SuppressWarnings("unused")
    public boolean isCall() {
        return this == StatementType.CALL_WITH_AUTO_COMMIT || this == StatementType.CALL_WITHOUT_AUTO_COMMIT;
    }

}
