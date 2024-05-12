package ru.jamsys.core.component;

import lombok.Setter;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.resource.PropertiesComponent;
import ru.jamsys.core.extension.RunnableComponent;
import ru.jamsys.core.util.*;


import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
@Lazy
public class Security implements RunnableComponent {

    @Setter
    private String pathStorage;

    @Setter
    private String pathPublicKey;

    @Setter
    private String pathInitAlias;

    final private String pathInitSecurityKeyJava;

    final private ExceptionHandler exceptionHandler;

    private volatile KeyStore keyStore = null;

    private char[] privateKey = {};

    private String hashPassword = null;

    private final String hashPasswordType = "SHA1";

    public String typeStorage = "JCEKS";

    private KeyStore.PasswordProtection keyStorePP;

    public Security(PropertiesComponent propertiesComponent, ExceptionHandler exceptionHandler) {
        this.pathStorage = propertiesComponent.getProperties("rj.security.path.storage", String.class);
        this.pathPublicKey = propertiesComponent.getProperties("rj.security.path.public.key", String.class);
        this.pathInitAlias = propertiesComponent.getProperties("rj.security.path.init", String.class);
        this.pathInitSecurityKeyJava = propertiesComponent.getProperties("rj.security.path.java", String.class);
        this.exceptionHandler = exceptionHandler;
    }

    @SuppressWarnings("unused")
    public void setPrivateKey(char[] newPrivateKey) {
        privateKey = newPrivateKey;
    }

    private void insertAliases(char[] password) {
        try {
            byte[] initJson = UtilFile.readBytes(pathInitAlias);
            if (initJson.length > 0) {
                String initString = new String(initJson, StandardCharsets.UTF_8);
                JsonEnvelope<Map<String, Object>> mapJsonEnvelope = UtilJson.toMap(initString);
                if (mapJsonEnvelope.getException() == null && !mapJsonEnvelope.getObject().isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> addAlias = (Map<String, Object>) mapJsonEnvelope.getObject().get("addAlias");
                    if (addAlias != null) {
                        for (String key : addAlias.keySet()) {
                            if (!"".equals(addAlias.get(key).toString())) {
                                add(key, addAlias.get(key).toString().toCharArray(), password);
                            }
                        }
                        save(password);
                    }
                }
            }
        } catch (Exception e) {
            exceptionHandler.handler(e);
        }
    }

    public Set<String> getAvailableAliases() {
        try {
            return new HashSet<>(Collections.list(keyStore.aliases()));
        } catch (Exception e) {
            exceptionHandler.handler(e);
        }
        return new HashSet<>();
    }

    private String getPasswordFromInfoJson(byte[] initJson) {
        if (initJson == null || initJson.length == 0) {
            throw new RuntimeException("File: [" + pathInitAlias + "] is empty");
        }
        String result = null;
        String initString = new String(initJson, StandardCharsets.UTF_8);
        JsonEnvelope<Map<String, Object>> mapJsonEnvelope = UtilJson.toMap(initString);
        if (mapJsonEnvelope.getException() == null && !mapJsonEnvelope.getObject().isEmpty()) {
            result = (String) mapJsonEnvelope.getObject().get("password");
        }
        if (result == null || result.trim().isEmpty()) {
            throw new RuntimeException("Password json field from [" + pathInitAlias + "] is empty");
        }
        return result;
    }

    private void printNotice(String password) {
        try {
            if (password != null && !password.trim().isEmpty()) {
                KeyPair keyPair = UtilRsa.genPair();
                byte[] token = UtilRsa.encrypt(keyPair, password.getBytes(StandardCharsets.UTF_8));
                UtilFile.writeBytes(pathPublicKey, token, FileWriteOptions.CREATE_OR_REPLACE);
                String privateKey = UtilBase64.base64Encode(keyPair.getPrivate().getEncoded(), true);
                System.err.println("== INIT SECURITY ===========================");
                byte[] securityKeyJava = UtilFileResource.get("SecurityKey.java").readAllBytes();
                UtilFile.writeBytes(
                        pathInitSecurityKeyJava,
                        new String(securityKeyJava).replace("{privateKey}", privateKey).getBytes(),
                        FileWriteOptions.CREATE_OR_REPLACE
                );
                System.err.println("Create file: [" + pathInitSecurityKeyJava + "] please restart application");
            } else {
                System.err.println("== INIT SECURITY ===========================");
                System.err.println("** Update file [" + pathInitAlias + "]; password field must not be empty");
            }
        } catch (Exception e) {
            throw new RuntimeException("Other problem", e);
        }
        System.err.println("== INIT SECURITY ===========================");
        throw new RuntimeException("Security.run() failed");
    }

    private byte[] decryptStoragePassword(byte[] token) {
        byte[] bytesPrivateKey = UtilBase64.base64DecodeResultBytes(UtilByte.charsToBytes(privateKey));
        if (bytesPrivateKey == null || bytesPrivateKey.length == 0) {
            throw new RuntimeException("Private key is empty");
        }
        byte[] bytesPasswordKeyStore;
        try {
            bytesPasswordKeyStore = UtilRsa.decrypt(UtilRsa.getPrivateKey(bytesPrivateKey), token);
        } catch (Exception e) {
            UtilFile.removeIfExist(pathPublicKey);
            throw new RuntimeException("Decrypt token exception. File: [" + pathPublicKey + "] removed, please restart application", e);
        }
        if (bytesPasswordKeyStore == null || bytesPasswordKeyStore.length == 0) {
            throw new RuntimeException("Decrypt Token empty. Change/remove token file: [" + pathPublicKey + "]");
        }
        return bytesPasswordKeyStore;
    }

    private byte[] createInitTemplateFile() {
        byte[] init;
        try {
            init = UtilFile.readBytes(pathInitAlias);
        } catch (FileNotFoundException | NoSuchFileException exception) {
            //Если нет - создадим
            System.err.println("== NEED INIT SECURITY ===========================");
            System.err.println("** Update file [" + pathInitAlias + "]");
            System.err.println("== NEED INIT SECURITY ===========================");
            try {
                init = UtilFileResource.get("security.json").readAllBytes();
                UtilFile.writeBytes(pathInitAlias, init, FileWriteOptions.CREATE_OR_REPLACE);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
            //Нет смысла продолжать работу, когда файл инициализации в данный момент пустой
            throw new RuntimeException("Update file [" + pathInitAlias + "]");
        } catch (IOException e) {
            //Если возникли другие проблемы при чтении файла инициализации прекратим работу
            throw new RuntimeException(e.getMessage(), e);
        }
        return init;
    }

    private boolean checkPassword(char[] password) {
        try {
            return hashPassword != null
                    && hashPassword.equals(
                    new String(
                            Util.getHash(UtilByte.charsToBytes(password), hashPasswordType),
                            StandardCharsets.UTF_8
                    ));
        } catch (Exception e) {
            exceptionHandler.handler(e);
        }
        return false;
    }

    public void loadKeyStorage(char[] password) throws Exception {
        if (password == null || password.length == 0) {
            throw new RuntimeException("Password is empty; Change/remove token file: [" + pathPublicKey + "]");
        }
        hashPassword = new String(Util.getHash(UtilByte.charsToBytes(password), hashPasswordType), StandardCharsets.UTF_8);
        keyStorePP = new KeyStore.PasswordProtection(password);
        File f = new File(pathStorage);
        if (!f.exists()) {
            keyStore = KeyStore.getInstance(typeStorage);
            keyStore.load(null, password);
            save(password);
        } else {
            try (InputStream stream = new ByteArrayInputStream(UtilFile.readBytes(pathStorage))) {
                keyStore = KeyStore.getInstance(typeStorage);
                keyStore.load(stream, password);
            } catch (Exception e) {
                keyStore = null;
                throw e;
            }
        }
    }

    public void add(String key, char[] value, char[] password) throws Exception {
        if (keyStore != null) {
            if (checkPassword(password)) {
                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBE");
                SecretKey generatedSecret = factory.generateSecret(new PBEKeySpec(value, "any".getBytes(), 13));
                keyStore.setEntry(key, new KeyStore.SecretKeyEntry(generatedSecret), keyStorePP);
                save(password);
            } else {
                throw new Exception("Не верный пароль");
            }
        } else {
            throw new Exception("Security компонент не инициализирован");
        }
    }

    @SuppressWarnings("unused")
    public boolean isInit() {
        return keyStore != null;
    }

    public char[] get(String key) throws Exception {
        if (keyStore != null) {
            KeyStore.SecretKeyEntry ske = (KeyStore.SecretKeyEntry) keyStore.getEntry(key, keyStorePP);
            if (ske != null) {
                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBE");
                PBEKeySpec keySpec = (PBEKeySpec) factory.getKeySpec(ske.getSecretKey(), PBEKeySpec.class);
                return keySpec.getPassword();
            }
            return null;
        } else {
            throw new Exception("Security компонент не инициализирован");
        }
    }

    public void remove(String key, char[] password) throws Exception {
        if (checkPassword(password)) {
            keyStore.deleteEntry(key);
            save(password);
        } else {
            throw new Exception("Не верный пароль");
        }
    }

    private void save(char[] password) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        keyStore.store(byteArrayOutputStream, password);
        UtilFile.writeBytes(pathStorage, byteArrayOutputStream.toByteArray(), FileWriteOptions.CREATE_OR_REPLACE);
    }

    @Deprecated
    public static void printStorage(char[] password, String pathStorage) {
        KeyStore.PasswordProtection keyStorePP = new KeyStore.PasswordProtection(password);
        File f = new File(pathStorage);
        KeyStore keyStore;
        String typeStorage = "JCEKS";
        if (f.exists()) {
            try (InputStream stream = new ByteArrayInputStream(UtilFile.readBytes(pathStorage))) {
                keyStore = KeyStore.getInstance(typeStorage);
                keyStore.load(stream, password);
                HashSet<String> strings = new HashSet<>(Collections.list(keyStore.aliases()));
                KeyStore finalKeyStore1 = keyStore;
                strings.forEach((String key) -> {
                    System.out.println(key);
                    try {
                        KeyStore.SecretKeyEntry ske = (KeyStore.SecretKeyEntry) finalKeyStore1.getEntry(key, keyStorePP);
                        if (ske != null) {
                            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBE");
                            PBEKeySpec keySpec = (PBEKeySpec) factory.getKeySpec(ske.getSecretKey(), PBEKeySpec.class);
                            System.out.println(keySpec.getPassword());
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

            } catch (Exception e) {
                App.context.getBean(ExceptionHandler.class).handler(e);
            }
        }else{
            Util.logConsole("file not exist");
        }
    }

    @Override
    public void run() {
        byte[] publicKey = UtilFile.readBytes(pathPublicKey, null);

        Util.logConsole("Security Check privateKey: " + (privateKey != null && privateKey.length > 0));
        Util.logConsole("Security Check publicKey: " + (publicKey != null && publicKey.length > 0));

        if (publicKey != null && publicKey.length > 0 && privateKey != null && privateKey.length > 0) {
            // У нас всё установлено можем просто работать
            byte[] passwordKeyStore = decryptStoragePassword(publicKey);
            if (passwordKeyStore.length == 0) {
                throw new RuntimeException("Decrypt password KeyStore is empty; Change/Remove [" + pathPublicKey + "]");
            }
            try {
                loadKeyStorage(UtilByte.bytesToChars(passwordKeyStore));
            } catch (Exception e) {
                throw new RuntimeException("Security.run() init exception", e);
            }
            if (UtilFile.ifExist(pathInitAlias)) {
                System.err.println("Please remove file [" + pathInitAlias + "] with credentials information");
                insertAliases(UtilByte.bytesToChars(passwordKeyStore));
            }
            try {
                Util.logConsole(
                        "KeyStore available aliases: "
                                + UtilJson.toStringPretty(getAvailableAliases(), "[]")
                );
            } catch (Exception ignore) {

            }
            setPrivateKey(new char[]{});
        } else {
            UtilFile.removeIfExist(pathStorage);
            //У нас чего-то не хватает выводим предупреждения
            byte[] initJson = createInitTemplateFile();
            String passwordFromInfoJson = getPasswordFromInfoJson(initJson);
            printNotice(passwordFromInfoJson);
        }
    }

    @Override
    public void shutdown() {

    }

}