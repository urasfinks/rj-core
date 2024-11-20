package ru.jamsys.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.UtilBase64;
import ru.jamsys.core.flat.util.UtilByte;
import ru.jamsys.core.flat.util.crypto.UtilRsa;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

// IO time: ms
// COMPUTE time: ms

class UtilRsaTest {

    @Test
    public void test() throws Exception {
        KeyPair keyPair = UtilRsa.genPair();
        byte[] secretByte = UtilRsa.encrypt(keyPair, "12345".getBytes(StandardCharsets.UTF_8));
        String privateKey = UtilBase64.base64Encode(keyPair.getPrivate().getEncoded(), false);
        byte[] bytes = UtilBase64.base64DecodeResultBytes(UtilByte.charsToBytes(privateKey.toCharArray()));
        String resultDecode = new String(UtilRsa.decrypt(UtilRsa.getPrivateKey(bytes), secretByte), StandardCharsets.UTF_8);
        Assertions.assertEquals("12345", resultDecode);
    }

    @Test
    public void sign() throws Exception {
        UtilRsa.size = 512;
        KeyPair keyPair = UtilRsa.genPair();
        String msg = "Hello world Hello world Hello world Hello world Hello world Hello world";
        String sig = UtilBase64.base64Encode(UtilRsa.sign(msg, keyPair.getPrivate()), false);
        System.out.println(sig);
        Assertions.assertTrue(UtilRsa.verify(msg, sig, keyPair.getPublic()));
    }

    private static byte[] zipB64(String x) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry(""));
            byte[] bytes = x.getBytes();
            zos.write(bytes, 0, bytes.length);
            zos.closeEntry();
        }
        return baos.toByteArray();
    }

}