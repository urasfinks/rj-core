package ru.jamsys.core.resource.jdbc;

import ru.jamsys.core.component.manager.sub.PoolResourceArgument;
import ru.jamsys.core.flat.template.jdbc.DefaultStatementControl;
import ru.jamsys.core.flat.template.jdbc.StatementControl;

public class JdbcPoolSetting {

    // пространство из *.properties
    public final String namespaceProperties;

    public final StatementControl statementControl = new DefaultStatementControl();

    public static PoolResourceArgument<
            ConnectionResource2,
            JdbcPoolSetting
            > setting = new PoolResourceArgument<>(ConnectionResource2.class, null, e -> {
        if (e != null) {
            String msg = e.getMessage();
            // Не конкурентная проверка
            return msg.contains("закрыто")
                    || msg.contains("close")
                    || msg.contains("Connection reset")
                    || msg.contains("Ошибка ввода/вывода при отправке бэкенду");
        }
        return false;
    });

    public JdbcPoolSetting(String namespaceProperties) {
        this.namespaceProperties = namespaceProperties;
    }

    public JdbcPoolSetting() {
        this.namespaceProperties = "rj";
    }

}
