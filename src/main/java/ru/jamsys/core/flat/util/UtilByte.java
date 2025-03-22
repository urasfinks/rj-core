package ru.jamsys.core.flat.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

// short 16 бит (2 байта) Диапазон значений: от -32,768 до 32,767 (включительно)
// int 32 бита (4 байта) Диапазон значений: от -2,147,483,648 до 2,147,483,647 (включительно)

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

    public static byte[] intToBytes(int value) {
        return new byte[]{(byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value};
    }

    public static int bytesToInt(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24) | ((bytes[1] & 0xFF) << 16) | ((bytes[2] & 0xFF) << 8) | ((bytes[3] & 0xFF));
    }

    public static byte[] shortToBytes(short s) {
        return ByteBuffer.allocate(2).putShort(s).array();
    }

    public static short bytesToShort(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getShort();
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
