package ru.jamsys.core.resource.jdbc;

import lombok.Getter;
import ru.jamsys.core.flat.template.jdbc.DefaultStatementControl;
import ru.jamsys.core.flat.template.jdbc.StatementControl;
import ru.jamsys.core.resource.NamespaceResourceConstructor;

@Getter
public class JdbcResourceConstructor extends NamespaceResourceConstructor {

    public final StatementControl statementControl = new DefaultStatementControl();

}
