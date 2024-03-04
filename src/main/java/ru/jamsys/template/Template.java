package ru.jamsys.template;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Template {

    public static List<TemplateItem> getParsedTemplate(String template) {
        Parser parser = new Parser();
        List<TemplateItem> result = new ArrayList<>();
        for (int i = 0; i < template.length(); i++) {
            String ch = template.substring(i, i + 1);
            parser.read(ch);
            if (parser.isTerminal()) {
                String flush = parser.flush();
                if (!flush.equals("")) {
                    result.add(new TemplateItem(true, flush));
                }
            } else if (parser.isFinish()) {
                String flush = parser.flush();
                if (!flush.equals("")) {
                    result.add(new TemplateItem(false, flush));
                }
            }
        }
        String flush = parser.flush();
        if (!flush.equals("")) {
            result.add(new TemplateItem(true, flush));
        } else if (parser.isParse()) {
            result.add(new TemplateItem(true, "$"));
        }
        return merge(result);
    }

    public static String template(List<TemplateItem> parsedTemplate, Map<String, String> args) {
        StringBuilder sb = new StringBuilder();
        for (TemplateItem templateItem : parsedTemplate) {
            if (templateItem.isStatic()) {
                sb.append(templateItem.getValue());
            } else {
                sb.append(args.get(templateItem.getValue()));
            }
        }
        return sb.toString();
    }

    public static String template(String template, Map<String, String> args) {
        return template(getParsedTemplate(template), args);
    }

    private static List<TemplateItem> merge(List<TemplateItem> input) {
        List<TemplateItem> result = new ArrayList<>();
        int index = 0;
        List<TemplateItem> tmp = new ArrayList<>();
        while (true) {
            if (index > input.size() - 1) {
                get(tmp, result);
                break;
            }
            TemplateItem templateItem = input.get(index++);
            if (!templateItem.isStatic()) {
                get(tmp, result);
                tmp.clear();
                result.add(templateItem);
            } else {
                tmp.add(templateItem);
            }
        }
        return result;
    }

    private static void get(List<TemplateItem> tmp, List<TemplateItem> result) {
        if (tmp.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (TemplateItem item : tmp) {
                sb.append(item.getValue());
            }
            result.add(new TemplateItem(true, sb.toString()));
        }
    }


}
