package ru.jamsys.core.flat.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.functional.ProcedureThrowing;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

public class Util {

    public static final ObjectMapper objectMapper = new ObjectMapper();

    static final String defaultCharset = "UTF-8";

    public static <T> void printArray(T[] arr) {
        logConsole(Util.class, Arrays.toString(arr));
    }

    public static <T, R> List<R> forEach(T[] array, Function<T, R> fn) {
        List<R> list = new ArrayList<>();
        for (T item : array) {
            R r = fn.apply(item);
            if (r != null) {
                list.add(r);
            }
        }
        return list;
    }

    public static void logConsoleJson(Class<?> cls, String title, Object data) {
        logConsole(
                cls,
                "Title: " + title + "; Json: " + UtilJson.toStringPretty(data, "--"),
                false
        );
    }

    public static void logConsoleJson(Class<?> cls, Object data) {
        logConsole(cls, UtilJson.toStringPretty(data, "--"), false);
    }

    public static void logConsole(Class<?> cls, String data) {
        logConsole(cls, data, false);
    }

    public static void logConsole(Class<?> cls, String data, boolean err) {
        logConsole(cls, Thread.currentThread(), data, err);
    }

    public static void logConsole(Class<?> cls, String data, boolean err, boolean newLine) {
        logConsole(cls, Thread.currentThread(), data, err, newLine);
    }

    public static void logConsole(Class<?> cls, Thread thread, String data, boolean err) {
        logConsole(cls, thread, data, err, true);
    }

    public static void logConsole(Class<?> cls, Thread thread, String data, boolean err, boolean newLine) {
        PrintStream ps = err ? System.err : System.out;
        if (newLine) {
            ps.println(UtilDate.msFormat(System.currentTimeMillis()) + "; Class: " + cls.getName() + "; Thread: " + thread.getName() + "; " + data);
        } else {
            ps.print(UtilDate.msFormat(System.currentTimeMillis()) + "; Class: " + cls.getName() + "; Thread: " + thread.getName() + "; " + data);
        }
    }

    public static void sleepMs(long ms) {
        try {
            Thread.sleep(ms);
            //TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException e) {
            App.error(e);
        }
    }

    public static String genUser() {
        return "u" + ThreadLocalRandom.current().nextInt(10000, 99999);
    }

    public static String genPassword() {

        /*
         * net user  168e4&Zx /add  Если пароль содержит амперсанд - то при добавлении через консоль считается что это команда и всё падает
         * */
        int length = 8;

        final char[] lowercase = "abcdefghijklmnopqrstuvwxyz".toCharArray();
        final char[] uppercase = "ABCDEFGJKLMNPRSTUVWXYZ".toCharArray();
        final char[] numbers = "0123456789".toCharArray();
        final char[] symbols = "!".toCharArray();
        final char[] allAllowed = "abcdefghijklmnopqrstuvwxyzABCDEFGJKLMNPRSTUVWXYZ0123456789!".toCharArray();

        //Use cryptographically secure random number generator
        Random random = new SecureRandom();

        StringBuilder password = new StringBuilder();

        for (int i = 0; i < length - 4; i++) {
            password.append(allAllowed[random.nextInt(allAllowed.length)]);
        }

        //Ensure password policy is met by inserting required random chars in random positions
        password.insert(random.nextInt(password.length()), lowercase[random.nextInt(lowercase.length)]);
        password.insert(random.nextInt(password.length()), uppercase[random.nextInt(uppercase.length)]);
        password.insert(random.nextInt(password.length()), numbers[random.nextInt(numbers.length)]);
        password.insert(random.nextInt(password.length()), symbols[random.nextInt(symbols.length)]);
        return "p" + password;
    }

    public static String stackTraceToString(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

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

    public static String urlEncode(String data, String charset) throws Exception {
        return URLEncoder.encode(data, charset);
    }

    public static String urlEncode(String data) throws Exception {
        return urlEncode(data, defaultCharset);
    }

    public static String urlDecode(String data, String charset) throws Exception {
        return URLDecoder.decode(data, charset);
    }

    public static String urlDecode(String data) throws Exception {
        return urlDecode(data, defaultCharset);
    }

    public static byte[] getHashByte(byte[] bytes, String hashType) throws NoSuchAlgorithmException {
        // MD2, MD5, SHA1, SHA-256, SHA-384, SHA-512
        java.security.MessageDigest crypt2 = java.security.MessageDigest.getInstance(hashType);
        crypt2.reset();
        crypt2.update(bytes);
        return crypt2.digest();
    }

    public static byte[] getHashByte(String data, String hashType, String charset) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        return getHashByte(data.getBytes(charset), hashType);
    }

    public static String getHash(String data, String hashType) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        BigInteger bigInt = new BigInteger(1, getHashByte(data, hashType, defaultCharset));
        StringBuilder result = new StringBuilder(bigInt.toString(16));
        while (result.length() < 32) {
            result.insert(0, "0");
        }
        return result.toString();
    }

    public static String getHash(String data, String hashType, String charset) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        BigInteger bigInt = new BigInteger(1, getHashByte(data, hashType, charset));
        StringBuilder result = new StringBuilder(bigInt.toString(16));
        while (result.length() < 32) {
            result.insert(0, "0");
        }
        return result.toString();
    }

    public static Integer random(Integer minimum, Integer maximum) {
        return minimum + ThreadLocalRandom.current().nextInt((maximum - minimum) + 1);
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

    public static String capitalize(String str) {
        return StringUtils.capitalize(str);
    }

    public static boolean isNumeric(String num) {
        try {
            Double.parseDouble(num);
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    public static boolean isInt(String num) {
        try {
            Integer.parseInt(num);
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    public static void printStackTrace(String label) {
        Exception exception = new Exception(
                UtilDate.msFormat(System.currentTimeMillis()) + " ["
                        + Thread.currentThread().getName() + "] "
                        + "Util.printStackTrace: " + label + "\r\n"
        );
        exception.printStackTrace();
    }

    public static void overflow(Map<String, Object> def, Map<String, Object> newObj) {
        for (String key : newObj.keySet()) {
            def.put(key, newObj.get(key));
        }
    }

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
            return firstCharToUpperCase(phrase);
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

    public static String htmlEntity(String value) {
        return escapeHtml4(value);
    }

    public static long zeroLastNDigits(long x, long n) {
        long tenToTheN = (long) Math.pow(10, n);
        return (x / tenToTheN) * tenToTheN;
    }

    public static String getIp() {
        String result = null;
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress("google.com", 80));
            result = socket.getLocalAddress().toString();
            socket.close();
        } catch (Exception e) {
            App.error(e);
        }
        return result;
    }

    public static int stringToInt(String data, int min, int max) {
        if (data == null) {
            return min;
        }
        int sum = 0;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            for (byte digestByte : md.digest(data.getBytes(StandardCharsets.UTF_8))) {
                sum += digestByte & 0xFF;
            }
        } catch (Exception e) {
            App.error(e);
        }
        int sumReverse = Integer.parseInt(new StringBuilder(sum + "").reverse().toString());
        double result = min + Math.floor(Double.parseDouble("0." + sumReverse) * (max - min + 1));
        return (int) result;
    }

    public static <T> Set<T> getConcurrentHashSet() {
        return ConcurrentHashMap.newKeySet();
    }

    public static boolean await(AtomicBoolean run, long timeoutMs, String exceptionMessage) {
        return await(run, timeoutMs, 0, () -> {
            if (exceptionMessage != null) {
                logConsole(Util.class, exceptionMessage, true);
            }
        });
    }

    public static boolean await(AtomicBoolean run, long timeoutMs, int sleepIterationMs, String exceptionMessage) {
        return await(run, timeoutMs, sleepIterationMs, () -> {
            if (exceptionMessage != null) {
                logConsole(Util.class, exceptionMessage, true);
            }
        });
    }

    public static boolean await(AtomicBoolean run, long timeoutMs, int sleepIterationMs, ProcedureThrowing procedure) {
        long start = System.currentTimeMillis();
        long expiredTime = start + timeoutMs;
        if (sleepIterationMs > 0) {
            while (run.get() && expiredTime >= System.currentTimeMillis()) {
                try {
                    Thread.sleep(sleepIterationMs);
                } catch (InterruptedException ie) {
                    break;
                }
            }
        } else {
            while (run.get() && expiredTime >= System.currentTimeMillis()) {
                Thread.onSpinWait();
            }
        }
        if (run.get()) {
            if (procedure != null) {
                try {
                    procedure.run();
                } catch (Throwable e) {
                    App.error(e);
                }
            }
            return false;
        } else {
            return true;
        }
    }

    @SuppressWarnings("unchecked")
    public static <R extends Collection<?>> R cartesian(Supplier nCol, Collection<?>... cols) {
        // проверка supplier не есть null
        if (nCol == null) return null;
        return (R) Arrays.stream(cols)
                // ненулевые и непустые коллекции
                .filter(col -> col != null && !col.isEmpty())
                // представить каждый элемент коллекции как одноэлементную коллекцию
                .map(col -> (Collection<Collection<?>>) col.stream()
                        .map(e -> Stream.of(e).collect(Collectors.toCollection(nCol)))
                        .collect(Collectors.toCollection(nCol)))
                // суммирование пар вложенных коллекций
                .reduce((col1, col2) -> (Collection<Collection<?>>) col1.stream()
                        // комбинации вложенных коллекций
                        .flatMap(inner1 -> col2.stream()
                                // объединить в одну коллекцию
                                .map(inner2 -> Stream.of(inner1, inner2)
                                        .flatMap(Collection::stream)
                                        .collect(Collectors.toCollection(nCol))))
                        // коллекция комбинаций
                        .collect(Collectors.toCollection(nCol)))
                // иначе пустая коллекция
                .orElse((Collection<Collection<?>>) nCol.get());
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

    public static String digitTranslate(int number, String one, String two, String five) {
        // Определяем последние две цифры для исключений типа 11, 12, 13, 14
        int lastTwoDigits = number % 100;
        // Определяем последнюю цифру
        int lastDigit = number % 10;
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

    public static <T> T mapToObject(Map<String, Object> map, Class<T> cls) {
        return objectMapper.convertValue(map, cls);
    }

}
