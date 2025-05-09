package ru.jamsys.core.flat.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.crypto.UtilRsa;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;

// IO time: ms
// COMPUTE time: ms

class UtilRsaTest {

    @Test
    public void test() throws Exception {
        KeyPair keyPair = UtilRsa.genKeyPair();
        byte[] secretByte = UtilRsa.encrypt("12345".getBytes(StandardCharsets.UTF_8), keyPair);
        String privateKey = UtilBase64.encode(keyPair.getPrivate().getEncoded(), false);
        byte[] bytes = UtilBase64.decodeResultBytes(UtilByte.charsToBytes(privateKey.toCharArray()));
        String resultDecode = new String(UtilRsa.decrypt(secretByte, UtilRsa.getPrivateKey(bytes)), StandardCharsets.UTF_8);
        Assertions.assertEquals("12345", resultDecode);
    }

    @Test
    public void sign() throws Exception {
        UtilRsa.size = 512;
        KeyPair keyPair = UtilRsa.genKeyPair();
        String msg = "Hello world Hello world Hello world Hello world Hello world Hello world";
        String sig = UtilBase64.encode(UtilRsa.sign(msg, keyPair.getPrivate()), false);
        UtilLog.printInfo(sig);
        Assertions.assertTrue(UtilRsa.verify(msg, sig, keyPair.getPublic()));
    }

}