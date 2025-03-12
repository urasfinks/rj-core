package ru.jamsys.core.flat.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class UtilLogConverter {

    public static String readShortString(InputStream fis) throws Exception {
        short len = UtilByte.bytesToShort(fis.readNBytes(2));
        return new String(fis.readNBytes(len), StandardCharsets.UTF_8);
    }

    public static String readString(InputStream fis) throws Exception {
        int len = UtilByte.bytesToInt(fis.readNBytes(4));
        return new String(fis.readNBytes(len), StandardCharsets.UTF_8);
    }

    public static void writeShortString(OutputStream fos, String data) throws Exception {
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        fos.write(UtilByte.shortToBytes((short) dataBytes.length));
        fos.write(dataBytes);
    }

    public static void writeString(OutputStream fos, String data) throws Exception {
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        fos.write(UtilByte.intToBytes(dataBytes.length));
        fos.write(dataBytes);
    }

}
