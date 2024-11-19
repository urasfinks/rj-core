package ru.jamsys.core.flat.util;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class UtilAes {

    public static String algorithm = "AES/CBC/PKCS5Padding";

    public static SecretKey generateKey(int n) throws NoSuchAlgorithmException {
        return generateKey(n, "AES");
    }

    public static SecretKey generateKey(int n, String alg) throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(alg);
        keyGenerator.init(n);
        return keyGenerator.generateKey();
    }

    public static IvParameterSpec generateIv() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return new IvParameterSpec(iv);
    }

    public static IvParameterSpec generateIv(byte[] iv) {
        return new IvParameterSpec(iv);
    }

    public static byte[] encrypt(
            String algorithm,
            byte[] input,
            SecretKey key,
            IvParameterSpec iv
    ) throws
            NoSuchPaddingException,
            NoSuchAlgorithmException,
            InvalidAlgorithmParameterException,
            InvalidKeyException,
            BadPaddingException,
            IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        return cipher.doFinal(input);
    }

    public static byte[] decrypt(
            String algorithm,
            byte[] cipherText,
            SecretKey key,
            IvParameterSpec iv
    ) throws
            NoSuchPaddingException,
            NoSuchAlgorithmException,
            InvalidAlgorithmParameterException,
            InvalidKeyException,
            BadPaddingException,
            IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        return cipher.doFinal(cipherText);
    }

    public static void encryptFile(
            String algorithm,
            SecretKey key,
            IvParameterSpec iv,
            InputStream inputStream,
            OutputStream outputStream
    ) throws
            IOException,
            NoSuchPaddingException,
            NoSuchAlgorithmException,
            InvalidAlgorithmParameterException,
            InvalidKeyException,
            BadPaddingException,
            IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);

        byte[] buffer = new byte[64];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            byte[] output = cipher.update(buffer, 0, bytesRead);
            if (output != null) {
                outputStream.write(output);
            }
        }
        byte[] outputBytes = cipher.doFinal();
        if (outputBytes != null) {
            outputStream.write(outputBytes);
        }
    }

}
