package ru.jamsys.core.flat.util;

import ru.jamsys.core.App;
import ru.jamsys.core.extension.functional.ProcedureThrowing;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.Socket;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

public class Util {

    static final String defaultCharset = "UTF-8";

    // Использовать только для тестов, так как проглатывается InterruptedException
    public static void testSleepMs(long ms) {
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

    @SuppressWarnings("all")
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
                UtilLog.printError(exceptionMessage);
            }
        });
    }

    public static boolean await(AtomicBoolean run, long timeoutMs, int sleepIterationMs, String exceptionMessage) {
        return await(run, timeoutMs, sleepIterationMs, () -> {
            if (exceptionMessage != null) {
                UtilLog.printError(exceptionMessage);
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

}
