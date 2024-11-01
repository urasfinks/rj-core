package ru.jamsys.core.flat.util;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;

public class UtilRsa {

    public static String alg = "RSA";
    public static String signAlg = "MD5withRSA";
    public static int size = 2048;

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

    public static String sign(String message, PrivateKey privateKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature signature = Signature.getInstance(signAlg);
        SecureRandom secureRandom = new SecureRandom();
        signature.initSign(privateKey, secureRandom);
        signature.update(message.getBytes());
        return UtilBase64.base64Encode(signature.sign(), false);
    }

    public static boolean verify(String message, String signature, PublicKey publicKey) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature publicSignature = Signature.getInstance(signAlg);
        publicSignature.initVerify(publicKey);
        publicSignature.update(message.getBytes());
        return publicSignature.verify(UtilBase64.base64DecodeResultBytes(signature));
    }

}
