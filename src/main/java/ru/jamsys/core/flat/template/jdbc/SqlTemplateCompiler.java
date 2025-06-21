package ru.jamsys.core.flat.template.jdbc;

import lombok.Getter;
import ru.jamsys.core.flat.template.twix.TemplateItemTwix;
import ru.jamsys.core.flat.template.twix.TemplateTwix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Компилятор для JdbcStatement. Закидывается map для наполнения sql шаблона, на выходе получаем подготовленный набор
// для statement состоящий из собранного sql с "?" и индексируемые аргументы со значениями и типами

@Getter
public class SqlTemplateCompiler {

    private String cache;

    private boolean dynamicArgument = false; // Если есть в аргументах динамичные фрагменты (IN_ENUM)

    private List<TemplateItemTwix> listTemplateItemTwix;

    private List<Argument> listArgument;

    public SqlTemplateCompiler(String sqlTemplate) {
        parse(sqlTemplate);
    }

    private void parse(String sql) {
        listTemplateItemTwix = TemplateTwix.getParsedTemplate(sql);
        listArgument = new ArrayList<>();
        for (TemplateItemTwix templateItemTwix : listTemplateItemTwix) {
            if (!templateItemTwix.isStatic) {
                Argument argument = Argument.getInstance(templateItemTwix.value);
                listArgument.add(argument);
                if (DynamicFragment.check(argument.getType())) {
                    dynamicArgument = true;
                }
            }
        }
    }

    public SqlTemplateCompiled compile(Map<String, Object> args) throws CloneNotSupportedException {
        Map<String, String> templateArgs = new HashMap<>();
        SqlTemplateCompiled sqlTemplateCompiled = new SqlTemplateCompiled();
        List<Argument> resultListArgument = sqlTemplateCompiled.getListArgument();
        int nextIndex = 1;
        for (Argument argument : listArgument) {
            Object argumentValue = args.get(argument.getKey());
            if (DynamicFragment.check(argument.getType())) {
                templateArgs.put(
                        argument.getKeySqlTemplate(),
                        DynamicFragment.compile(argument.getType(), argumentValue, argument.getKey())
                );
                for (Object obj : (List<?>) argumentValue) {
                    resultListArgument.add(new Argument(
                            ArgumentDirection.IN,
                            DynamicFragment.mapType.get(argument.getType()),
                            argument.getKey(),
                            argument.getKeySqlTemplate()
                    )
                            .setIndex(nextIndex++)
                            .setValue(obj));
                }
            } else {
                templateArgs.put(argument.getKeySqlTemplate(), "?");
                resultListArgument.add(new Argument(
                        argument.getDirection(),
                        argument.getType(),
                        argument.getKey(),
                        argument.getKeySqlTemplate()
                )
                        .setIndex(nextIndex++)
                        .setValue(argumentValue));
            }
        }
        if (cache == null && !dynamicArgument) {
            cache = TemplateTwix.template(listTemplateItemTwix, templateArgs);
        }
        if (dynamicArgument) {
            sqlTemplateCompiled.setSql(TemplateTwix.template(listTemplateItemTwix, templateArgs));
        } else {
            sqlTemplateCompiled.setSql(cache);
        }
        return sqlTemplateCompiled;
    }

}
