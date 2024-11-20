package ru.jamsys.core.flat.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class UtilCompressTest {

    @Test
    void gzip() throws IOException {
        String data = "Hello world";
        byte[] bytes = UtilCompress.compressGzip(data.getBytes());
        Assertions.assertEquals(data, new String(UtilCompress.decompressGzip(bytes)));
    }

    @Test
    void zip() throws IOException {
        String data = "Hello world";
        byte[] bytes = UtilCompress.compressZip(data.getBytes(), "1.txt");
        Assertions.assertEquals(data, new String(UtilCompress.decompressZip(bytes, "1.txt")));
    }

    @Test
    void base() throws IOException {
        String data = "Hello world";
        byte[] bytes = UtilCompress.compressBase(data.getBytes());
        Assertions.assertEquals(data, new String(UtilCompress.decompressBase(bytes)));
    }

}