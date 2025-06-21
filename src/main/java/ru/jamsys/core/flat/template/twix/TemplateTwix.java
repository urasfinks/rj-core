package ru.jamsys.core.flat.template.twix;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TemplateTwix {

    public static List<TemplateItemTwix> getParsedTemplate(String template) {
        Parser parser = new Parser();
        List<TemplateItemTwix> result = new ArrayList<>();
        for (int i = 0; i < template.length(); i++) {
            String ch = template.substring(i, i + 1);
            parser.read(ch);
            if (parser.isTerminal()) {
                String flush = parser.flush();
                if (!flush.isEmpty()) {
                    result.add(new TemplateItemTwix(true, flush));
                }
            } else if (parser.isFinish()) {
                String flush = parser.flush();
                if (!flush.isEmpty()) {
                    result.add(new TemplateItemTwix(false, flush));
                }
            }
        }
        String flush = parser.flush();
        if (!flush.isEmpty()) {
            result.add(new TemplateItemTwix(true, flush));
        } else if (parser.isParse()) {
            result.add(new TemplateItemTwix(true, "$"));
        }
        return merge(result);
    }

    public static String template(List<TemplateItemTwix> parsedTemplate, Map<String, String> args) {
        return template(parsedTemplate, args, false);
    }

    public static String template(List<TemplateItemTwix> parsedTemplate, Map<String, String> args, boolean forwardVariableIfNull) {
        StringBuilder sb = new StringBuilder();
        for (TemplateItemTwix templateItemTwix : parsedTemplate) {
            if (templateItemTwix.isStaticFragment()) {
                sb.append(templateItemTwix.getValue());
            } else {
                String value = args.get(templateItemTwix.getValue());
                if (forwardVariableIfNull && value == null) {
                    sb
                            .append("${")
                            .append(templateItemTwix.getValue())
                            .append("}");
                } else {
                    sb.append(value);
                }

            }
        }
        return sb.toString();
    }

    public static String template(String template, Map<String, String> args) {
        return template(template, args, false);
    }

    public static String template(String template, Map<String, String> args, boolean forwardVariableIfNull) {
        return template(getParsedTemplate(template), args, forwardVariableIfNull);
    }

    private static List<TemplateItemTwix> merge(List<TemplateItemTwix> input) {
        List<TemplateItemTwix> result = new ArrayList<>();
        int index = 0;
        List<TemplateItemTwix> tmp = new ArrayList<>();
        while (true) {
            if (index > input.size() - 1) {
                get(tmp, result);
                break;
            }
            TemplateItemTwix templateItemTwix = input.get(index++);
            if (!templateItemTwix.isStaticFragment()) {
                get(tmp, result);
                tmp.clear();
                result.add(templateItemTwix);
            } else {
                tmp.add(templateItemTwix);
            }
        }
        return result;
    }

    private static void get(List<TemplateItemTwix> tmp, List<TemplateItemTwix> result) {
        if (!tmp.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (TemplateItemTwix item : tmp) {
                sb.append(item.getValue());
            }
            result.add(new TemplateItemTwix(true, sb.toString()));
        }
    }


}
