package ru.jamsys.core.flat.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Base64;

@SuppressWarnings("unused")
public class UtilBase64 {

    public static byte[] encodeResultBytes(byte[] data) {
        return Base64.getEncoder().encode(data);
    }

    public static byte[] encodeResultBytes(String data, String dataCharset) {
        return encodeResultBytes(data.getBytes(Charset.forName(dataCharset)));
    }

    public static byte[] encodeResultBytes(String data) {
        return encodeResultBytes(data, Util.defaultCharset);
    }

    public static String encode(byte[] data, boolean multiline) {
        return multiline
                ? Base64.getMimeEncoder().encodeToString(data)
                : new String(encodeResultBytes(data), Charset.forName(Util.defaultCharset));
    }

    public static String encode(String data, String dataCharset, boolean multiline) {
        byte[] bytes = data.getBytes(Charset.forName(dataCharset));
        return encode(bytes, multiline);
    }

    public static String encode(String data, boolean multiline) {
        return encode(data, Util.defaultCharset, multiline);
    }

    public static byte[] decodeResultBytes(byte[] data) {
        return Base64.getMimeDecoder().decode(data);
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

    public static InputStream decode(InputStream is, boolean multiline) {
        return multiline
                ? Base64.getMimeDecoder().wrap(is)
                : Base64.getDecoder().wrap(is);
    }

    public static void decode(InputStream is, OutputStream os, boolean multiline) throws IOException {
        InputStream wrap = multiline
                ? Base64.getMimeDecoder().wrap(is)
                : Base64.getDecoder().wrap(is);
        wrap.transferTo(os);
        wrap.close();
    }

    public static void encode(InputStream is, OutputStream os, boolean multiline) throws IOException {
        OutputStream wrappedOs = multiline ?
                Base64.getMimeEncoder().wrap(os)
                : Base64.getEncoder().wrap(os);
        is.transferTo(wrappedOs);
        wrappedOs.close();
    }

    // Не смог сделать base64Encode: InputStream -> InputStream, так как wrap() возвращает OutputStream
    // А что бы OutputStream превратить в InputStream нужно накопить все байты после записи
    // Не вижу целесообразности такого метода, но вообще странно, почему нельзя реализовать последовательное чтение
    // При чтение из выходного стрима - прочитать входной стрим + кодировать и вернуть)
    // Да при кодировании может увеличиваться кол-во байт, ну сделать буфер и при следующем чтении отдать из буфера
    // без чтения входного стрима (НЕ ПОНЯТНО)

}
