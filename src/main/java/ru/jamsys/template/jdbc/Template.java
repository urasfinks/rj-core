package ru.jamsys.template.jdbc;

import lombok.Getter;
import ru.jamsys.App;
import ru.jamsys.component.ExceptionHandler;
import ru.jamsys.template.twix.TemplateItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Template {

    private String cache;

    @Getter
    private final StatementType statementType;

    private boolean dynamicArgument = false;

    private List<TemplateItem> listTemplateItem;

    private List<Argument> listArgument;

    public Template(String sql, StatementType statementType) throws Exception {
        this.statementType = statementType;
        parse(sql);
    }

    @SuppressWarnings("unused")
    public String getSql() {
        try {
            CompiledSqlTemplate compiledSqlTemplate = compile(new HashMap<>());
            return compiledSqlTemplate.getSql();
        } catch (Exception e) {
            App.context.getBean(ExceptionHandler.class).handler(e);
        }
        return null;
    }

    private void parse(String sql) throws Exception {
        listTemplateItem = ru.jamsys.template.twix.Template.getParsedTemplate(sql);
        listArgument = new ArrayList<>();
        for (TemplateItem templateItem : listTemplateItem) {
            if (!templateItem.isStatic) {
                Argument argument = new Argument();
                argument.parseSqlKey(templateItem.value);
                listArgument.add(argument);
                if (argument.getType().isDynamicFragment()) {
                    dynamicArgument = true;
                }
            }
        }
        analyze();
    }

    private void analyze() throws Exception {
        for (Argument argument : listArgument) {
            if (statementType.isSelect() && (argument.getDirection() == ArgumentDirection.OUT || argument.getDirection() == ArgumentDirection.IN_OUT)) {
                throw new Exception("Нельзя использовать OUT переменные в простых выборках");
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
            cache = ru.jamsys.template.twix.Template.template(listTemplateItem, templateArgs);
        }
        if (dynamicArgument) {
            compiledSqlTemplate.setSql(ru.jamsys.template.twix.Template.template(listTemplateItem, templateArgs));
        } else {
            compiledSqlTemplate.setSql(cache);
        }
        return compiledSqlTemplate;
    }

    public String debug(CompiledSqlTemplate compiledSqlTemplate) {
        String[] split = compiledSqlTemplate.getSql().split("\\?");
        StringBuilder sb = new StringBuilder();
        List<Argument> listArgument = compiledSqlTemplate.getListArgument();
        for (int i = 0; i < split.length; i++) {
            sb.append(split[i]);
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
                App.context.getBean(ExceptionHandler.class).handler(e);
            }
        }
        return sb.toString();
    }

}
