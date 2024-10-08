package ru.jamsys.core.flat.template.jdbc;

import lombok.Getter;
import ru.jamsys.core.App;
import ru.jamsys.core.flat.template.twix.TemplateItemTwix;
import ru.jamsys.core.flat.template.twix.TemplateTwix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JdbcTemplate {

    private String cache;

    @Getter
    private final StatementType statementType;

    @Getter
    private boolean dynamicArgument = false;

    private List<TemplateItemTwix> listTemplateItemTwix;

    private List<Argument> listArgument;

    public JdbcTemplate(String sql, StatementType statementType) {
        this.statementType = statementType;
        parse(sql);
    }

    @SuppressWarnings("unused")
    public String getSql() {
        try {
            CompiledSqlTemplate compiledSqlTemplate = compile(new HashMap<>());
            return compiledSqlTemplate.getSql();
        } catch (Exception e) {
            App.error(e);
        }
        return null;
    }

    private void parse(String sql) {
        listTemplateItemTwix = TemplateTwix.getParsedTemplate(sql);
        listArgument = new ArrayList<>();
        for (TemplateItemTwix templateItemTwix : listTemplateItemTwix) {
            if (!templateItemTwix.isStatic) {
                Argument argument = new Argument();
                argument.parseSqlKey(templateItemTwix.value);
                listArgument.add(argument);
                if (argument.getType().isDynamicFragment()) {
                    dynamicArgument = true;
                }
            }
        }
        analyze();
    }

    private void analyze() {
        for (Argument argument : listArgument) {
            if (statementType.isSelect() && (argument.getDirection() == ArgumentDirection.OUT || argument.getDirection() == ArgumentDirection.IN_OUT)) {
                throw new RuntimeException("Нельзя использовать OUT переменные в простых выборках");
            }
        }
    }

    public CompiledSqlTemplate compile(Map<String, Object> args) throws CloneNotSupportedException {
        CompiledSqlTemplate compiledSqlTemplate = new CompiledSqlTemplate();
        Map<String, String> templateArgs = new HashMap<>();
        int newIndex = 1;
        for (Argument argument : listArgument) {
            ArgumentType argumentType = argument.getType();
            Object argumentValue = args.get(argument.getKey());
            List<Argument> resultListArgument = compiledSqlTemplate.getListArgument();
            if (argumentType.isDynamicFragment()) {
                templateArgs.put(argument.getSqlKeyTemplate(), argumentType.compileDynamicFragment(argumentValue, argument.key));
                for (Object obj : (List<?>) argumentValue) {
                    Argument clone = argument.clone();
                    clone.setValue(obj);
                    clone.setIndex(newIndex++);
                    clone.setType(clone.getType().getRealType());
                    resultListArgument.add(clone);
                }
            } else {
                templateArgs.put(argument.getSqlKeyTemplate(), "?");
                Argument clone = argument.clone();
                clone.setValue(argumentValue);
                clone.setIndex(newIndex++);
                resultListArgument.add(clone);
            }
        }
        if (cache == null && !dynamicArgument) {
            cache = TemplateTwix.template(listTemplateItemTwix, templateArgs);
        }
        if (dynamicArgument) {
            compiledSqlTemplate.setSql(TemplateTwix.template(listTemplateItemTwix, templateArgs));
        } else {
            compiledSqlTemplate.setSql(cache);
        }
        return compiledSqlTemplate;
    }

    @SuppressWarnings("unused")
    public String getSqlWithArgumentsValue(CompiledSqlTemplate compiledSqlTemplate) {
        String sqlPrep = compiledSqlTemplate.getSql();
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

        List<Argument> listArgument = compiledSqlTemplate.getListArgument();
        for (int i = 0; i < split.length; i++) {
            sb.append(split[i]);
            if (i == split.length - 1) { //Пропускаем последний элемент
                continue;
            }
            try {
                Argument argument = listArgument.get(i);
                Object value = argument.getValue();
                if (argument.type.isString() && value != null) {
                    value = "'" + value + "'";
                }
                if (argument.direction == ArgumentDirection.OUT) {
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
