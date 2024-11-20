package ru.jamsys.core.flat.util.crypto;

import ru.jamsys.core.flat.util.UtilBase64;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;

public class UtilRsa {

    public static String alg = "RSA";
    public static String signAlg = "MD5withRSA";
    public static int size = 2048;

    public static KeyPair genKeyPair(int size) throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(alg);
        generator.initialize(size);
        return generator.generateKeyPair();
    }

    public static KeyPair genKeyPair() throws NoSuchAlgorithmException {
        return genKeyPair(size);
    }

    public static byte[] encrypt(byte[] input, KeyPair keyPair) throws Exception {
        Cipher rsa;
        rsa = Cipher.getInstance(alg);
        rsa.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
        return rsa.doFinal(input);
    }

    public static byte[] decrypt(byte[] input, PrivateKey keyPair) throws Exception {
        Cipher rsa = Cipher.getInstance(alg);
        rsa.init(Cipher.DECRYPT_MODE, keyPair);
        return rsa.doFinal(input);
    }

    public static PrivateKey getPrivateKey(byte[] encodedKey) throws Exception {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance(alg);
        return keyFactory.generatePrivate(keySpec);
    }

    public static byte[] sign(String message, PrivateKey privateKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature signature = Signature.getInstance(signAlg);
        SecureRandom secureRandom = new SecureRandom();
        signature.initSign(privateKey, secureRandom);
        signature.update(message.getBytes());
        return signature.sign();
    }

    public static boolean verify(String message, String signature, PublicKey publicKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature publicSignature = Signature.getInstance(signAlg);
        publicSignature.initVerify(publicKey);
        publicSignature.update(message.getBytes());
        return publicSignature.verify(UtilBase64.decodeResultBytes(signature));
    }

}
