package ru.jamsys.component;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.AbstractCoreComponent;
import ru.jamsys.FileWriteOptions;
import ru.jamsys.UtilBase64;
import ru.jamsys.UtilFile;

import javax.crypto.Cipher;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;

@Component
@Lazy
public class Secret extends AbstractCoreComponent {

    @Setter
    @Value("${rj.core.secret.path:security/security.jks.password.txt}")
    private String path;

    private static char[] privateKey;

    public static String alg = "RSA";

    public static void setPrivateKey(char[] newPrivateKey) {
        privateKey = newPrivateKey;
    }

    public static KeyPair genPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(alg);
        generator.initialize(512);
        return generator.generateKeyPair();
    }

    public static byte[] encrypt(KeyPair keyPair, String text) {
        try {
            Cipher rsa;
            rsa = Cipher.getInstance(alg);
            rsa.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
            return rsa.doFinal(text.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static PrivateKey getPrivateKey(byte[] encoded) throws Exception {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        KeyFactory keyFactory = KeyFactory.getInstance(alg);
        return keyFactory.generatePrivate(keySpec);
    }

    public static String decrypt(PrivateKey keyPair, byte[] buffer) {
        try {
            Cipher rsa;
            rsa = Cipher.getInstance(alg);
            rsa.init(Cipher.DECRYPT_MODE, keyPair);
            byte[] utf8 = rsa.doFinal(buffer);
            return new String(utf8, StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void flushStatistic() {

    }

    public byte[] charsToBytes(char[] chars) {
        ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(CharBuffer.wrap(chars));
        return Arrays.copyOf(byteBuffer.array(), byteBuffer.limit());
    }

    public char[] bytesToChars(byte[] bytes) {
        CharBuffer charBuffer = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes));
        return Arrays.copyOf(charBuffer.array(), charBuffer.limit());
    }

    @Override
    public void run() {
        try {
            byte[] securityPasswordPub = null;
            try {
                securityPasswordPub = UtilFile.readBytes(path);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (
                    securityPasswordPub == null
                            || securityPasswordPub.length == 0
                            || privateKey == null
                            || privateKey.length == 0
            ) {
                KeyPair keyPair = Secret.genPair();
                byte[] secretByte = Secret.encrypt(keyPair, "1234567891011121314");
                UtilFile.writeBytes(path, secretByte, FileWriteOptions.CREATE_OR_REPLACE);
                String privateKey = UtilBase64.base64Encode(keyPair.getPrivate().getEncoded(), false);
                System.err.println("-------------<PLEASE INIT SECRET>-------------------");
                System.err.println("/* Update static variable Secret.privateKey */");
                System.err.println("Secret.setPrivateKey(\"" + privateKey + "\".toCharArray());\r\n\r\n");
                System.err.println("-------------</LEASE INIT SECRET>------------------");
                //byte[] bytes = UtilBase64.base64DecodeResultBytes(charsToBytes(privateKey.toCharArray()));
                //System.out.println(Secret.decrypt(getPrivateKey(bytes), secretByte));
            } else {
                byte[] bytes = UtilBase64.base64DecodeResultBytes(charsToBytes(privateKey));
                System.out.println(Secret.decrypt(getPrivateKey(bytes), securityPasswordPub));
                privateKey = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
