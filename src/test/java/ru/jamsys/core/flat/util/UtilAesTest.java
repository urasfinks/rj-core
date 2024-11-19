package ru.jamsys.core.flat.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

class UtilAesTest {

    @Test
    public void test1() throws Exception {
        String input = "Hello world";
        SecretKey key = UtilAes.generateKey(128);
        IvParameterSpec ivParameterSpec = UtilAes.generateIv();
        byte[] encrypt = UtilAes.encrypt(UtilAes.algorithm, input.getBytes(), key, ivParameterSpec);
        System.out.println(UtilBase64.base64Encode(encrypt, false));
        byte[] decrypt = UtilAes.decrypt(UtilAes.algorithm, encrypt, key, UtilAes.generateIv(ivParameterSpec.getIV()));
        Assertions.assertEquals(input, new String(decrypt));
    }

}