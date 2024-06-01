package ru.jamsys.core.flat.util;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

public class UtilLog {

    public static String shortReadString(FileInputStream fis) throws Exception {
        short len = UtilByte.bytesToShort(fis.readNBytes(2));
        return new String(fis.readNBytes(len), StandardCharsets.UTF_8);
    }

    public static String readString(FileInputStream fis) throws Exception {
        int len = UtilByte.bytesToInt(fis.readNBytes(4));
        return new String(fis.readNBytes(len), StandardCharsets.UTF_8);
    }

    public static void shortWriteString(BufferedOutputStream fos, String data, AtomicInteger writeByte) throws Exception {
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        fos.write(UtilByte.shortToBytes((short) dataBytes.length));
        fos.write(dataBytes);
        writeByte.addAndGet(dataBytes.length + 2);
    }

    public static void writeString(BufferedOutputStream fos, String data, AtomicInteger writeByte) throws Exception {
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        fos.write(UtilByte.intToBytes(dataBytes.length));
        fos.write(dataBytes);
        writeByte.addAndGet(dataBytes.length + 4);
    }

}
