package ru.jamsys.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.UtilBase64;
import ru.jamsys.core.flat.util.UtilByte;
import ru.jamsys.core.flat.util.crypto.UtilRsa;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;

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
        KeyPair keyPair = UtilRsa.genPair();
        String msg = "Hello world Hello world Hello world Hello world Hello world Hello world";
        String sig = UtilRsa.sign(msg, keyPair.getPrivate());
        System.out.println(sig);
        Assertions.assertTrue(UtilRsa.verify(msg, sig, keyPair.getPublic()));
    }

}