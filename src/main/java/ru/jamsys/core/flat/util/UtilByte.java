package ru.jamsys.core.flat.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

// byte (8 бит) Диапазон значений: от -128 до 127 (включительно)
// short 16 бит (2 байта) Диапазон значений: от -32,768 до 32,767 (включительно)
// int 32 бита (4 байта) Диапазон значений: от -2,147,483,648 до 2,147,483,647 (включительно)
// long 64 бита	(8 байт) Диапазон значений: от -9 223 372 036 854 775 808 до 9 223 372 036 854 775 807

public class UtilByte {

    public static final long KB = 1024; // 1 килобайт
    public static final long MB = KB * 1024; // 1 мегабайт
    public static final long GB = MB * 1024; // 1 гигабайт
    public static final long TB = GB * 1024; // 1 терабайт

    public static long kilobytesToBytes(long kilobytes) {
        return kilobytes * KB;
    }

    public static long megabytesToBytes(long megabytes) {
        return megabytes * MB;
    }

    public static long gigabytesToBytes(long gigabytes) {
        return gigabytes * GB;
    }

    public static long terabytesToBytes(long terabytes) {
        return terabytes * TB;
    }

    public static byte[] charsToBytes(char[] chars) {
        ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(chars));
        return Arrays.copyOf(byteBuffer.array(), byteBuffer.limit());
    }

    public static char[] bytesToChars(byte[] bytes) {
        CharBuffer charBuffer = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes));
        return Arrays.copyOf(charBuffer.array(), charBuffer.limit());
    }

    public static byte[] shortToBytes(short s) {
        return ByteBuffer.allocate(2).putShort(s).array();
    }

    public static short bytesToShort(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getShort();
    }

    public static byte[] intToBytes(int value) {
        return ByteBuffer.allocate(4).putInt(value).array();
    }

    public static int bytesToInt(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }

    public static byte[] longToBytes(long value) {
        return ByteBuffer.allocate(8).putLong(value).array();
    }

    public static long bytesToLong(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getLong();
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

    /**
     * Метод для получения битов числа в виде строки.
     *
     * @param number Число типа short.
     * @return Строка, представляющая биты числа.
     */
    public static String getBits(short number) {
        StringBuilder bits = new StringBuilder();
        for (int i = 15; i >= 0; i--) {
            int bit = (number >> i) & 1;
            bits.append(bit);
        }
        return bits.toString();
    }

    public static String getBits(byte b) {
        StringBuilder bits = new StringBuilder(8);
        for (int i = 7; i >= 0; i--) {
            bits.append((b >> i) & 1);
        }
        return bits.toString();
    }

    /**
     * Метод для получения значения конкретного бита.
     *
     * @param number Исходное число.
     * @param bitIndex Индекс бита (начиная с 0).
     * @return Значение бита (0 или 1).
     */
    public static int getBit(short number, int bitIndex) {
        // Сдвигаем число вправо на bitIndex позиций и применяем побитовое AND с 1
        return (number >> bitIndex) & 1;
    }

    /**
     * Метод для установки конкретного бита в 1.
     *
     * @param number Исходное число.
     * @param bitIndex Индекс бита, который нужно установить в 1 (начиная с 0).
     * @return Число с измененным битом.
     */
    public static short setBit(short number, int bitIndex) {
        // Создаем маску: 1 сдвинутое на bitIndex позиций влево
        short mask = (short) (1 << bitIndex);
        // Устанавливаем бит с помощью побитового OR
        return (short) (number | mask);
    }

    public static short clearBit(short number, int bitIndex) {
        short mask = (short) ~(1 << bitIndex); // Инвертированная маска
        return (short) (number & mask);
    }

}
