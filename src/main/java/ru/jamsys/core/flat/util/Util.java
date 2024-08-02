package ru.jamsys.core.flat.util;

import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;
import ru.jamsys.core.App;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {

    static final String defaultCharset = "UTF-8";

    public static <T> void printArray(T[] arr) {
        logConsole(Arrays.toString(arr));
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

    public static void logConsole(String data) {
        logConsole(data, false);
    }

    public static void logConsole(String data, boolean err) {
        logConsole(Thread.currentThread(), data, err);
    }

    public static void logConsole(Thread t, String data, boolean err) {
        PrintStream out = err ? System.err : System.out;
        out.println(Util.msToDataFormat(System.currentTimeMillis()) + " thread: " + t.getName() + "; " + data);
    }

    public static long getTimestamp() {
        return System.currentTimeMillis() / 1000;
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

    public static String getDate(String format) {
        Date now = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(now);
    }

    public static long getTimestamp(String date, String format) throws Exception {
        DateFormat dateFormat = new SimpleDateFormat(format);
        Date d1 = dateFormat.parse(date);
        return d1.getTime() / 1000;
    }

    public static long getTimestampFromPostgreSql(String date) throws Exception {
        return getTimestamp(date, "yyyy-MM-dd HH:mm:ss.SSSX");
    }

    public static boolean dateValidate(String date, String format) {
        DateFormat DATE_FORMAT = new SimpleDateFormat(format);
        DATE_FORMAT.setLenient(true);
        try {
            return DATE_FORMAT.format(DATE_FORMAT.parse(date)).equals(date);
        } catch (Exception e) {
            App.error(e);
        }
        return false;
    }

    public static String timestampToDateFormat(long timestamp, String format) {
        Timestamp stamp = new Timestamp(timestamp * 1000);
        return new SimpleDateFormat(format).format(new Date(stamp.getTime()));
    }

    public static String msToDataFormat(Long ms) {
        if (ms == null) {
            return "null";
        }
        LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault());
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
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

    public static byte[] getHash(byte[] bytes, String hashType) throws NoSuchAlgorithmException {
        // MD2, MD5, SHA1, SHA-256, SHA-384, SHA-512
        java.security.MessageDigest crypt2 = java.security.MessageDigest.getInstance(hashType);
        crypt2.reset();
        crypt2.update(bytes);
        return crypt2.digest();
    }

    public static byte[] getHash(String data, String hashType, String charset) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        return getHash(data.getBytes(charset), hashType);
    }

    public static String getHash(String data, String hashType) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        return new String(getHash(data, hashType, defaultCharset), defaultCharset);
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

    public static String getResourceContent(Resource resource, String charset) {
        try (Reader reader = new InputStreamReader(resource.getInputStream(), charset)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void printStackTrace(String label) {
        Exception exception = new Exception(
                Util.msToDataFormat(System.currentTimeMillis()) + " ["
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

    public static String snakeToCamel(String phrase) {
        String[] s = phrase.split("_");
        StringBuilder sb = new StringBuilder();
        for (String w : s) {
            sb.append(w.substring(0, 1).toUpperCase()).append(w.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    public static String camelToSnake(String phrase) {
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

    public static String ucword(String someString) {
        return someString.substring(0, 1).toUpperCase() + someString.substring(1);
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

    public static  <T> Set<T> getConcurrentHashSet(){
        return ConcurrentHashMap.newKeySet();
    }

    public static boolean await(AtomicBoolean isRun, long timeoutMs, String exceptionMessage) {
        long start = System.currentTimeMillis();
        long expiredTime = start + timeoutMs;
        while (isRun.get() && expiredTime >= System.currentTimeMillis()) {
            Thread.onSpinWait();
        }
        if (isRun.get()) {
            if (exceptionMessage != null) {
                logConsole(exceptionMessage, true);
            }
            return false;
        } else {
            return true;
        }
    }

}
