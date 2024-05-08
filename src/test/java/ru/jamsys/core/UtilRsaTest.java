package ru.jamsys.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.util.UtilBase64;
import ru.jamsys.core.util.UtilByte;
import ru.jamsys.core.util.UtilRsa;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;

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

}