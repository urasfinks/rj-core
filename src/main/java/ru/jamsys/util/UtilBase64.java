package ru.jamsys.util;

import java.nio.charset.Charset;
import java.util.Base64;

@SuppressWarnings("unused")
public class UtilBase64 {

    public static byte[] base64EncodeResultBytes(byte[] data) {
        return Base64.getEncoder().encode(data);
    }

    public static byte[] base64EncodeResultBytes(String data, String dataCharset) {
        return base64EncodeResultBytes(data.getBytes(Charset.forName(dataCharset)));
    }

    public static byte[] base64EncodeResultBytes(String data) {
        return base64EncodeResultBytes(data, Util.defaultCharset);
    }

    public static String base64Encode(byte[] data, boolean multiline) {
        return multiline
                ? Base64.getMimeEncoder().encodeToString(data)
                : new String(base64EncodeResultBytes(data), Charset.forName(Util.defaultCharset));
    }

    public static String base64Encode(String data, String dataCharset, boolean multiline) {
        byte[] bytes = data.getBytes(Charset.forName(dataCharset));
        return base64Encode(bytes, multiline);
    }

    public static String base64Encode(String data, boolean multiline) {
        return base64Encode(data, Util.defaultCharset, multiline);
    }

    public static byte[] base64DecodeResultBytes(byte[] data) {
        return Base64.getMimeDecoder().decode(data);
    }

    public static byte[] base64DecodeResultBytes(String data, String dataCharset) {
        return base64DecodeResultBytes(data.getBytes(Charset.forName(dataCharset)));
    }

    public static byte[] base64DecodeResultBytes(String data) {
        return base64DecodeResultBytes(data, Util.defaultCharset);
    }

    public static String base64Decode(byte[] data) {
        return new String(base64DecodeResultBytes(data), Charset.forName(Util.defaultCharset));
    }

    public static String base64Decode(String data, String dataCharset) {
        return base64Decode(data.getBytes(Charset.forName(dataCharset)));
    }

    public static String base64Decode(String data) {
        return base64Decode(data, Util.defaultCharset);
    }

}
