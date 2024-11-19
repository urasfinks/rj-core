package ru.jamsys.core.flat.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UtilXorTest {

    @Test
    void testString() {

        String value = "SampleStringToBeEncrypted";
        String key = "thisIsAKey";

        String encrypt = UtilXor.encrypt(value, key);
        System.out.println(encrypt);
        Assertions.assertEquals("270904032516123f17101a0f3d1c0b160425060b0d181d162d", encrypt);

        Assertions.assertEquals(value, UtilXor.decrypt(encrypt, key));

    }

    @Test
    void testByte(){
        String value = "SampleStringToBeEncrypted";
        String key = "thisIsAKey";
        byte[] bytes = UtilXor.encrypt(value.getBytes(), key);
        Assertions.assertEquals("JwkEAyUWEj8XEBoPPRwLFgQlBgsNGB0WLQ==", UtilBase64.base64Encode(bytes, false));

        Assertions.assertEquals(value, new String(UtilXor.decrypt(bytes, key)));
    }

}