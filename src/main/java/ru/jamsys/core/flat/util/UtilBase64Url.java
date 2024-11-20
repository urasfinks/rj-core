package ru.jamsys.core.flat.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Base64;

@SuppressWarnings("unused")
public class UtilBase64Url {

    public static byte[] encodeResultBytes(byte[] data) {
        return Base64.getUrlEncoder().encode(data);
    }

    public static byte[] encodeResultBytes(String data, String dataCharset) {
        return encodeResultBytes(data.getBytes(Charset.forName(dataCharset)));
    }

    public static byte[] encodeResultBytes(String data) {
        return encodeResultBytes(data, Util.defaultCharset);
    }

    public static String encode(byte[] data) {
        return Base64.getUrlEncoder().encodeToString(data);
    }

    public static String encode(String data, String dataCharset) {
        byte[] bytes = data.getBytes(Charset.forName(dataCharset));
        return encode(bytes);
    }

    public static String encode(String data) {
        return encode(data, Util.defaultCharset);
    }

    public static byte[] decodeResultBytes(byte[] data) {
        return Base64.getUrlDecoder().decode(data);
    }

    public static byte[] decodeResultBytes(String data, String dataCharset) {
        return decodeResultBytes(data.getBytes(Charset.forName(dataCharset)));
    }

    public static byte[] decodeResultBytes(String data) {
        return decodeResultBytes(data, Util.defaultCharset);
    }

    public static String decode(byte[] data) {
        return new String(decodeResultBytes(data), Charset.forName(Util.defaultCharset));
    }

    public static String decode(String data, String dataCharset) {
        return decode(data.getBytes(Charset.forName(dataCharset)));
    }

    public static String decode(String data) {
        return decode(data, Util.defaultCharset);
    }

    // Stream

    public static InputStream decode(InputStream is) {
        return Base64.getUrlDecoder().wrap(is);
    }

    public static void decode(InputStream is, OutputStream os) throws IOException {
        InputStream wrap = Base64.getUrlDecoder().wrap(is);
        wrap.transferTo(os);
        wrap.close();
    }

    public static void encode(InputStream is, OutputStream os) throws IOException {
        OutputStream wrappedOs = Base64.getUrlEncoder().wrap(os);
        is.transferTo(wrappedOs);
        wrappedOs.close();
    }

}
