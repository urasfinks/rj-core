package ru.jamsys.core.flat;

import ru.jamsys.core.flat.util.UtilText;

public class UtilCodeStyle {
    public static boolean isSnakeCase(String input) {
        return input.contains("_");
    }

    public static boolean isCamelCase(String input) {
        boolean hasUpper = false;
        boolean hasLower = false;

        for (char c : input.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUpper = true;
            }
            if (Character.isLowerCase(c)) {
                hasLower = true;
            }
            // Если нашли оба, можно завершить цикл
            if (hasUpper && hasLower) {
                return true;
            }
        }
        return false;
    }

    public static String snakeToCamel(String phrase) {
        if (phrase == null) {
            return null;
        }
        String[] s = phrase.split("_");
        if (s.length == 1 && isCamelCase(phrase)) {
            return UtilText.firstCharToUpperCase(phrase);
        }
        StringBuilder sb = new StringBuilder();
        for (String w : s) {
            if (w == null || w.isEmpty()) {
                sb.append("_");
                continue;
            }
            sb.append(w.substring(0, 1).toUpperCase())
                    .append(w.substring(1).toLowerCase());
        }
        if (phrase.endsWith("_")) {
            sb.append("_");
        }
        return sb.toString();
    }

    public static String camelToSnake(String phrase) {
        if (phrase == null) {
            return null;
        }
        if (isSnakeCase(phrase)) {
            return phrase.toUpperCase();
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < phrase.length(); i++) {
            char c = phrase.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append("_");
            }
            sb.append(c);
        }
        return sb.toString().toUpperCase();
    }

}
