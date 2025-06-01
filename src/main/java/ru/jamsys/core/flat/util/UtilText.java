package ru.jamsys.core.flat.util;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UtilText {

    public static String padRight(String data, int n) {
        if (data == null) {
            return "";
        }
        return String.format("%-" + n + "s", data);
    }

    public static String padRight(String data, int n, String ch) {
        if (data == null) {
            return "";
        }
        if (ch == null) ch = " ";
        int lenStr = data.length();
        if (lenStr >= n) {
            return data;
        } else {
            return data + ch.repeat(n - lenStr);
        }
    }

    public static String padLeft(String data, int n) {
        return String.format("%" + n + "s", data);
    }

    public static String padLeft(String data, int n, String ch) {
        if (data == null) {
            return "";
        }
        if (ch == null) ch = " ";
        int lenStr = data.length();
        if (lenStr >= n) {
            return data;
        } else {
            StringBuilder sb = new StringBuilder(data);
            int addLen = n - lenStr;
            for (int i = 0; i < addLen; i++) {
                sb.insert(0, ch);
            }
            return sb.toString();
        }
    }

    public static String trimLeft(String data, String ch) {
        StringBuilder sb = new StringBuilder(data);
        while (!sb.isEmpty() && ch.equals(sb.charAt(0) + "")) {
            sb.delete(0, 1);
        }
        return sb.toString();
    }

    public static String trimRight(String data, String ch) {
        StringBuilder sb = new StringBuilder(data);
        while (!sb.isEmpty() && ch.equals(sb.charAt(sb.length() - 1) + "")) {
            sb.delete(sb.length() - 1, sb.length());
        }
        return sb.toString();
    }

    public static String capitalize(String str) {
        return StringUtils.capitalize(str);
    }

    public static String regexpReplace(String data, String pattern, String replace) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(data);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, replace);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public static String regexpFind(String str, String pattern) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(str);
        return m.find() ? m.group(0) : null;
    }

    public static String firstCharToLowerCase(String someString) {
        if (someString == null || someString.isEmpty()) {
            return someString;
        }
        if (someString.length() == 1) {
            return someString.toLowerCase();
        }
        return someString.substring(0, 1).toLowerCase() + someString.substring(1);
    }

    public static String firstCharToUpperCase(String someString) {
        if (someString == null || someString.isEmpty()) {
            return someString;
        }
        if (someString.length() == 1) {
            return someString.toUpperCase();
        }
        return someString.substring(0, 1).toUpperCase() + someString.substring(1);
    }

    public static String digitTranslate(int number, String one, String two, String five) {
        return digitTranslate((long) number, one, two, five);
    }
    public static String digitTranslate(long number, String one, String two, String five) {
        // Определяем последние две цифры для исключений типа 11, 12, 13, 14
        long lastTwoDigits = number % 100;
        // Определяем последнюю цифру
        long lastDigit = number % 10;
        if (lastTwoDigits >= 11 && lastTwoDigits <= 14) {
            return five;
        } else if (lastDigit == 1) {
            return one;
        } else if (lastDigit >= 2 && lastDigit <= 4) {
            return two;
        } else {
            return five;
        }
    }

    public static String readUntil(String data, Function<String, Boolean> fn) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length(); i++) {
            if (fn.apply(data.charAt(i) + "")) {
                sb.append(data.charAt(i));
                continue;
            }
            break;
        }
        return sb.toString();
    }

    public static List<String> stringToList(String string, String patternDelimiter) {
        if (string == null || string.isEmpty()) {
            return Collections.emptyList();
        }
        if (patternDelimiter == null || patternDelimiter.isEmpty()) {
            return List.of(string);
        }

        List<String> result = new ArrayList<>();
        int start = 0;
        int end;

        while ((end = string.indexOf(patternDelimiter, start)) != -1) {
            String token = string.substring(start, end).trim();
            if (!token.isEmpty()) {
                result.add(token);
            }
            start = end + patternDelimiter.length();
        }

        // добавляем остаток строки
        String lastToken = string.substring(start).trim();
        if (!lastToken.isEmpty()) {
            result.add(lastToken);
        }

        return result;
    }

}
