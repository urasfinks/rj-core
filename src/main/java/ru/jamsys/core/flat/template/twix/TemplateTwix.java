package ru.jamsys.core.flat.template.twix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TemplateTwix {

    public static List<TemplateItemTwix> getParsedTemplate(String template) {
        if (template == null) {
            return Collections.emptyList();
        }

        Parser parser = new Parser();
        List<TemplateItemTwix> result = new ArrayList<>();

        for (int i = 0; i < template.length(); i++) {
            char ch = template.charAt(i); // более эффективно
            parser.read(Character.toString(ch));

            if (parser.isTerminal()) {
                String flush = parser.flush();
                if (!flush.isEmpty()) {
                    result.add(new TemplateItemTwix(true, flush));
                }
            } else if (parser.isFinished()) {
                String flush = parser.flush();
                if (!flush.isEmpty()) {
                    result.add(new TemplateItemTwix(false, flush));
                }
            }
        }

        String flush = parser.flush();
        if (!flush.isEmpty()) {
            result.add(new TemplateItemTwix(true, flush));
        } else if (parser.isParsing()) {
            result.add(new TemplateItemTwix(true, "$")); // неудачно завершённый парсинг?
        }

        return mergeStaticFragments(result);
    }

    public static String template(List<TemplateItemTwix> parsedTemplate, Map<String, String> args) {
        return template(parsedTemplate, args, false);
    }

    public static String template(
            List<TemplateItemTwix> parsedTemplate,
            Map<String, String> args,
            boolean forwardVariableIfNull
    ) {
        StringBuilder sb = new StringBuilder();

        for (TemplateItemTwix item : parsedTemplate) {
            if (item.isStaticFragment()) {
                sb.append(item.getValue());
            } else {
                String value = args.get(item.getValue());
                if (forwardVariableIfNull && value == null) {
                    sb.append("${").append(item.getValue()).append("}");
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

    private static List<TemplateItemTwix> mergeStaticFragments(List<TemplateItemTwix> input) {
        List<TemplateItemTwix> result = new ArrayList<>();
        List<TemplateItemTwix> staticBuffer = new ArrayList<>();

        for (TemplateItemTwix item : input) {
            if (!item.isStaticFragment()) {
                flushStaticBuffer(staticBuffer, result);
                result.add(item);
            } else {
                staticBuffer.add(item);
            }
        }

        flushStaticBuffer(staticBuffer, result);
        return result;
    }

    private static void flushStaticBuffer(List<TemplateItemTwix> staticBuffer, List<TemplateItemTwix> result) {
        if (!staticBuffer.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (TemplateItemTwix item : staticBuffer) {
                sb.append(item.getValue());
            }
            result.add(new TemplateItemTwix(true, sb.toString()));
            staticBuffer.clear();
        }
    }

}
