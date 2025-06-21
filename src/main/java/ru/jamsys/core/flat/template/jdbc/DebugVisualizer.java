package ru.jamsys.core.flat.template.jdbc;

import ru.jamsys.core.App;

import java.util.ArrayList;
import java.util.List;

public interface DebugVisualizer {
    // Получить собранный sql для отладки. Нельзя использовать для реальных запросов!!!!
    static String get(SqlTemplateCompiled sqlTemplateCompiled) {
        String sqlPrep = sqlTemplateCompiled.getSql();
        String[] split = sqlPrep.split("\\?");
        if (sqlPrep.endsWith("?")) {
            ArrayList<String> strings = new ArrayList<>(List.of(split));
            strings.add("");
            split = strings.toArray(new String[0]);
        }

        StringBuilder sb = new StringBuilder();
        if (split.length == 1) {
            sb.append(split[0]);
            return sb.toString();
        }

        List<Argument> listArgument = sqlTemplateCompiled.getListArgument();
        for (int i = 0; i < split.length; i++) {
            sb.append(split[i]);
            if (i == split.length - 1) { //Пропускаем последний элемент
                continue;
            }
            try {
                Argument argument = listArgument.get(i);
                Object value = argument.getValue();
                if (argument.getType() == ArgumentType.VARCHAR && value != null) {
                    value = "'" + value + "'";
                }
                if (argument.getDirection() == ArgumentDirection.OUT) {
                    value = "${OUT}";
                }
                sb.append(value == null ? "null" : value);
            } catch (Exception e) {
                App.error(e);
            }
        }
        return sb.toString();
    }

}
