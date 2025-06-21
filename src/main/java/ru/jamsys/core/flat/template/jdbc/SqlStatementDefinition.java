package ru.jamsys.core.flat.template.jdbc;


public interface SqlStatementDefinition {

    SqlTemplateCompiler getSqlTemplateCompiler();

    SqlExecutionMode getSqlExecutionMode();

    default void analyze() {
        for (Argument argument : getSqlTemplateCompiler().getArguments()) {
            if (getSqlExecutionMode().isSelect() && (argument.getDirection() == ArgumentDirection.OUT || argument.getDirection() == ArgumentDirection.IN_OUT)) {
                throw new RuntimeException("Нельзя использовать OUT переменные в простых выборках");
            }
        }
    }

}
