package ru.jamsys.core.flat.util;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;

public class UtilRsa {

    public static String alg = "RSA";
    public static int size = 512;

    public static KeyPair genPair(int size) throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(alg);
        generator.initialize(size);
        return generator.generateKeyPair();
    }

    public static KeyPair genPair() throws NoSuchAlgorithmException {
        return genPair(size);
    }

    public static byte[] encrypt(KeyPair keyPair, byte[] bytes) throws Exception {
        Cipher rsa;
        rsa = Cipher.getInstance(alg);
        rsa.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
        return rsa.doFinal(bytes);
    }

    public static byte[] decrypt(PrivateKey keyPair, byte[] buffer) throws Exception {
        Cipher rsa = Cipher.getInstance(alg);
        rsa.init(Cipher.DECRYPT_MODE, keyPair);
        return rsa.doFinal(buffer);
    }

    public static PrivateKey getPrivateKey(byte[] encoded) throws Exception {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        KeyFactory keyFactory = KeyFactory.getInstance(alg);
        return keyFactory.generatePrivate(keySpec);
    }

}
