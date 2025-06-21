package ru.jamsys.core.flat.template.jdbc;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SqlTemplateCompiled {
    private List<Argument> listArgument = new ArrayList<>();
    private String sql;
}
