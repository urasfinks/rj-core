package ru.jamsys.core.flat.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.crypto.UtilAes;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

class UtilAesTest {

    @Test
    public void test1() throws Exception {
        String input = "100776404158ZNSW&2024-10-10T15:28";
        SecretKey key = UtilAes.generateKey(128);
        IvParameterSpec ivParameterSpec = UtilAes.generateIv();
        byte[] encrypt = UtilAes.encrypt(UtilAes.algorithm, input.getBytes(), key, ivParameterSpec);
        System.out.println(UtilBase64.encode(encrypt, false));
        byte[] decrypt = UtilAes.decrypt(UtilAes.algorithm, encrypt, key, UtilAes.generateIv(ivParameterSpec.getIV()));
        Assertions.assertEquals(input, new String(decrypt));
    }

}