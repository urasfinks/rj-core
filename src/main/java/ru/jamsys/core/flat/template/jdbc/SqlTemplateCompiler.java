package ru.jamsys.core.flat.template.jdbc;

import lombok.Getter;
import ru.jamsys.core.flat.template.twix.TemplateItemTwix;
import ru.jamsys.core.flat.template.twix.TemplateTwix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Компилирует SQL-шаблон с параметрами, преобразуя их в подготовленный SQL-запрос и список аргументов.
 */

@Getter
public class SqlTemplateCompiler {

    private final String originalTemplate;

    private String cache;

    private boolean dynamicArgument = false;

    private List<TemplateItemTwix> templateItems;

    private List<Argument> arguments;

    public SqlTemplateCompiler(String sqlTemplate) {
        this.originalTemplate = sqlTemplate;
        parse(sqlTemplate);
    }

    private void parse(String sql) {
        templateItems = TemplateTwix.getParsedTemplate(sql);
        arguments = new ArrayList<>();

        for (TemplateItemTwix item : templateItems) {
            if (!item.isStaticFragment()) {
                Argument argument = Argument.getInstance(item.getValue());
                arguments.add(argument);

                if (DynamicFragment.check(argument.getType())) {
                    dynamicArgument = true;
                }
            }
        }
    }

    public SqlTemplateCompiled compile(Map<String, Object> args) throws CloneNotSupportedException {
        Map<String, String> resolvedFragments = new HashMap<>();
        SqlTemplateCompiled result = new SqlTemplateCompiled();
        List<Argument> compiledArguments = result.getListArgument();
        int index = 1;

        for (Argument argument : arguments) {
            Object value = args.get(argument.getKey());

            if (DynamicFragment.check(argument.getType())) {
                if (value == null) {
                    // Убираем фрагмент SQL, если значение null
                    resolvedFragments.put(argument.getKeySqlTemplate(), "");
                    continue;
                }

                if (!(value instanceof List<?> valueList)) {
                    throw new IllegalArgumentException("Expected a List for dynamic argument: " + argument.getKey());
                }

                resolvedFragments.put(
                        argument.getKeySqlTemplate(),
                        DynamicFragment.compile(argument.getType(), valueList, argument.getKey())
                );

                for (Object obj : valueList) {
                    compiledArguments.add(
                            new Argument(
                                    ArgumentDirection.IN,
                                    DynamicFragment.mapType.get(argument.getType()),
                                    argument.getKey(),
                                    argument.getKeySqlTemplate()
                            ).setIndex(index++)
                                    .setValue(obj)
                    );
                }
            } else {
                resolvedFragments.put(argument.getKeySqlTemplate(), "?");

                compiledArguments.add(
                        new Argument(
                                argument.getDirection(),
                                argument.getType(),
                                argument.getKey(),
                                argument.getKeySqlTemplate()
                        ).setIndex(index++)
                                .setValue(value) // null допустим
                );
            }
        }

        // Генерация SQL: используем кэш, если шаблон статичный
        if (cache == null && !dynamicArgument) {
            cache = TemplateTwix.template(templateItems, resolvedFragments);
        }

        String finalSql = dynamicArgument
                ? TemplateTwix.template(templateItems, resolvedFragments)
                : cache;

        result.setSql(finalSql);
        return result;
    }
}

