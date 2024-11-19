package ru.jamsys.core.flat.util.crypto;

import java.io.ByteArrayOutputStream;

public class UtilXor {

    public static String encrypt(String msg, String key) {
        StringBuilder sb = new StringBuilder(msg.length() * 2);
        int keyItr = 0;
        for (int i = 0; i < msg.length(); i++) {
            int temp = msg.charAt(i) ^ key.charAt(keyItr);
            sb.append(String.format("%02x", (byte) temp));
            keyItr++;
            if (keyItr >= key.length()) {
                keyItr = 0;
            }
        }
        return sb.toString();
    }

    public static byte[] encrypt(byte[] msg, String key) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int keyItr = 0;
        for (byte b : msg) {
            int temp = b ^ key.charAt(keyItr);
            baos.write((byte) temp);
            keyItr++;
            if (keyItr >= key.length()) {
                keyItr = 0;
            }
        }
        return baos.toByteArray();
    }

    public static String decrypt(String encryptedMessage, String key) {
        int keyItr = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < encryptedMessage.length() - 1; i += 2) {
            int temp = ((char) Integer.parseInt(encryptedMessage.substring(i, (i + 2)), 16)) ^ key.charAt(keyItr);
            sb.append((char) temp);
            keyItr++;
            if (keyItr >= key.length()) {
                keyItr = 0;
            }
        }
        return sb.toString();
    }

    public static byte[] decrypt(byte[] encryptedMessage, String key) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int keyItr = 0;
        for (byte b : encryptedMessage) {
            //XOR Operation
            baos.write(b ^ key.charAt(keyItr));
            keyItr++;
            if (keyItr >= key.length()) {
                keyItr = 0;
            }
        }
        return baos.toByteArray();
    }
}
