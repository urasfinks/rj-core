package ru.jamsys.core.flat.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

// short 16 бит (2 байта) Диапазон значений: от -32,768 до 32,767 (включительно)
// int 32 бита (4 байта) Диапазон значений: от -2,147,483,648 до 2,147,483,647 (включительно)
// TODO: UtilLogConverter -> UtilFileByteReader
public class UtilLogConverter {

    // Читаем 2 байта длины данных, потом читаем столько байт данных
    public static String readShortString(InputStream fis) throws Exception {
        short len = UtilByte.bytesToShort(fis.readNBytes(2));
        return new String(fis.readNBytes(len), StandardCharsets.UTF_8);
    }

    // Читаем 4 байта длины данных, потом читаем столько байт данных
    public static String readString(InputStream fis) throws Exception {
        int len = UtilByte.bytesToInt(fis.readNBytes(4));
        return new String(fis.readNBytes(len), StandardCharsets.UTF_8);
    }

    // Пишем в 2 байта длину данных, потом пишем байты самих данных
    public static void writeShortString(OutputStream fos, String data) throws Exception {
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        fos.write(UtilByte.shortToBytes((short) dataBytes.length));
        fos.write(dataBytes);
    }

    // Пишем в 4 байта длину данных, потом пишем байты самих данных
    public static void writeString(OutputStream fos, String data) throws Exception {
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        fos.write(UtilByte.intToBytes(dataBytes.length));
        fos.write(dataBytes);
    }

}
