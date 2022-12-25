package ru.jamsys;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {

    static final String defaultCharset = "UTF-8";

    @SuppressWarnings("unused")
    public static <T> void printArray(T[] arr) {
        System.out.println(Arrays.toString(arr));
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

    public static void logConsole(Thread t, String data) {
        System.out.println(LocalDateTime.now().toString() + " " + t.getName() + " " + data);
    }

    @SuppressWarnings("unused")
    public static long getTimestamp() {
        return System.currentTimeMillis() / 1000;
    }

    static ObjectMapper objectMapper = new ObjectMapper();

    @Nullable
    public static String jsonObjectToString(Object o) {

        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Nullable
    public static String jsonObjectToStringPretty(Object o) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static ObjectMapper objectMapper2 = new ObjectMapper();

    public static <T> WrapJsonToObject<T> jsonToObjectOverflowProperties(String json, Class<T> t) {
        WrapJsonToObject<T> ret = new WrapJsonToObject<>();
        try {
            objectMapper2.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            ret.setObject(objectMapper2.readValue(json, t));
        } catch (Exception e) {
            ret.setException(e);
            e.printStackTrace();
        }
        return ret;
    }

    public static <T> WrapJsonToObject<T> jsonToObject(String json, Class<T> t) {
        WrapJsonToObject<T> ret = new WrapJsonToObject<>();
        try {
            ret.setObject(objectMapper.readValue(json, t));
        } catch (Exception e) {
            ret.setException(e);
            e.printStackTrace();
        }
        return ret;
    }

    public static String getApplicationProperties(String key) throws Exception {
        throw new Exception("Deprecated");
    }

    public static void sleepMillis(int millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
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
        return "p" + password.toString();
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
            StringBuffer sb = new StringBuffer(data);
            int addLen = n - lenStr;
            for (int i = 0; i < addLen; i++) {
                sb.append(ch);
            }
            return sb.toString();
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
            StringBuffer sb = new StringBuffer(data);
            int addLen = n - lenStr;
            for (int i = 0; i < addLen; i++) {
                sb.insert(0, ch);
            }
            return sb.toString();
        }
    }

    public static String trimLeft(String data, String ch) {
        StringBuffer sb = new StringBuffer(data);
        while (sb.length() > 0 && ch.equals(sb.charAt(0) + "")) {
            sb.delete(0, 1);
        }
        return sb.toString();
    }

    public static String trimRight(String data, String ch) {
        StringBuffer sb = new StringBuffer(data);
        while (sb.length() > 0 && ch.equals(sb.charAt(sb.length() - 1) + "")) {
            sb.delete(sb.length() - 1, sb.length());
        }
        return sb.toString();
    }

    public static String getDate(String format) {
        Date now = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(now);
    }

    public static String getTimestamp(String date, String format) throws Exception {
        DateFormat dateFormat = new SimpleDateFormat(format);
        Date d1 = dateFormat.parse(date);
        long unixTime = d1.getTime() / 1000;
        return Long.toString(unixTime);
    }

    public static boolean dateValidate(String date, String format) {
        DateFormat DATE_FORMAT = new SimpleDateFormat(format);
        DATE_FORMAT.setLenient(true);
        try {
            return DATE_FORMAT.format(DATE_FORMAT.parse(date)).equals(date);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
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

    public static byte[] getHash(String data, String hashType, String charset) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        // MD2, MD5, SHA1, SHA-256, SHA-384, SHA-512
        byte[] bytes = data.getBytes(charset);
        java.security.MessageDigest crypt2 = java.security.MessageDigest.getInstance(hashType);
        crypt2.reset();
        crypt2.update(bytes);
        return crypt2.digest();
    }

    public static String getHash(String data, String hashType) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        return new String(getHash(data, hashType, defaultCharset), defaultCharset);
    }

    public static Integer random(Integer minimum, Integer maximum) {
        Random rand = new Random();
        return minimum + rand.nextInt((maximum - minimum) + 1);
    }

    public static void reverseBytes(byte[] array) {
        int i = 0;
        int j = array.length - 1;
        byte tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }

    public static String regexpReplace(String data, String pattern, String replace) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(data);
        StringBuffer sb = new StringBuffer();
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
        } catch (Exception e) {
        }
        return false;
    }

    public static boolean isInt(String num) {
        try {
            Integer.parseInt(num);
            return true;
        } catch (Exception e) {
        }
        return false;
    }

}
