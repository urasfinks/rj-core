package ru.jamsys.core.flat.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class UtilCompressTest {

    @Test
    void gzip() throws IOException {
        String data = "Hello world";
        byte[] bytes = UtilCompress.gzip(data.getBytes());
        Assertions.assertEquals(data, new String(UtilCompress.unGzip(bytes)));
    }

    @Test
    void zip() throws IOException {
        String data = "Hello world";
        byte[] bytes = UtilCompress.zip(data.getBytes(), "1.txt");
        Assertions.assertEquals(data, new String(UtilCompress.unZip(bytes, "1.txt")));
    }

    @Test
    void base() throws IOException {
        String data = "Hello world";
        byte[] bytes = UtilCompress.base(data.getBytes());
        Assertions.assertEquals(data, new String(UtilCompress.unBase(bytes)));
    }

}