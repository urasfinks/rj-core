package ru.jamsys.core.component;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.AbstractLifeCycle;
import ru.jamsys.core.extension.LifeCycleComponent;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.flat.util.*;
import ru.jamsys.core.flat.util.crypto.UtilRsa;

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

@FieldNameConstants
@Component
public class SecurityComponent extends AbstractLifeCycle implements LifeCycleComponent {

    @Getter
    private final SecurityComponentProperty property = new SecurityComponentProperty();

    private volatile KeyStore keyStore = null;

    private char[] privateKey = {};

    private String hashPassword = null;

    private final String hashPasswordType = "SHA1";

    public String typeStorage = "JCEKS";

    private KeyStore.PasswordProtection keyStorePP;

    private final PropertyDispatcher<String> propertyDispatcher;

    public SecurityComponent() {
        propertyDispatcher = new PropertyDispatcher<>(
                null,
                property,
                null
        );
    }

    @SuppressWarnings("unused")
    public void setPrivateKey(char[] newPrivateKey) {
        privateKey = newPrivateKey;
    }

    private void updateDataFromJsonCred(char[] password) {
        try {
            byte[] initJson = UtilFile.readBytes(property.getPathJsonCred());
            if (initJson.length > 0) {
                String initString = new String(initJson, StandardCharsets.UTF_8);
                Map<String, Object> mapOrThrow = UtilJson.getMapOrThrow(initString);
                if (!mapOrThrow.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> addAlias = (Map<String, Object>) mapOrThrow.get("addAlias");
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
        } catch (Throwable th) {
            App.error(th);
        }
    }

    public Set<String> getAvailableAliases() {
        try {
            return new HashSet<>(Collections.list(keyStore.aliases()));
        } catch (Exception e) {
            App.error(e);
        }
        return new HashSet<>();
    }

    private String getPasswordFromJsonCred(byte[] initJson) {
        if (initJson == null || initJson.length == 0) {
            throw new RuntimeException("File: [" + property.getPathJsonCred() + "] is empty");
        }
        String result = null;
        String initString = new String(initJson, StandardCharsets.UTF_8);
        Map<String, Object> mapOrThrow;
        try {
            mapOrThrow = UtilJson.getMapOrThrow(initString);
        } catch (Throwable th) {
            throw new ForwardException(th);
        }
        if (!mapOrThrow.isEmpty()) {
            result = (String) mapOrThrow.get("password");
        }
        if (result == null || result.trim().isEmpty()) {
            throw new RuntimeException("Password json field from [" + property.getPathJsonCred() + "] is empty");
        }
        return result;
    }

    private void printNotice(String password) {
        try {
            if (password != null && !password.trim().isEmpty()) {
                KeyPair keyPair = UtilRsa.genKeyPair();
                byte[] token = UtilRsa.encrypt(password.getBytes(StandardCharsets.UTF_8), keyPair);
                UtilFile.writeBytes(property.getPathPublicKey(), token, FileWriteOptions.CREATE_OR_REPLACE);
                String privateKey = UtilBase64.encode(keyPair.getPrivate().getEncoded(), true);
                System.err.println("== INIT SECURITY ===========================");
                byte[] securityKeyJava = UtilFileResource.get("SecurityKey.java").readAllBytes();
                UtilFile.writeBytes(
                        property.getPathInitSecurityKeyJava(),
                        new String(securityKeyJava).replace("{privateKey}", privateKey).getBytes(),
                        FileWriteOptions.CREATE_OR_REPLACE
                );
                System.err.println("Create file: [" + property.getPathInitSecurityKeyJava() + "] please restart application");
                new Thread(() -> {
                    Util.testSleepMs(2_000);
                    System.exit(-1);
                }).start();
            } else {
                System.err.println("== INIT SECURITY ===========================");
                System.err.println("** Update file [" + property.getPathJsonCred() + "]; password field must not be empty");
            }
        } catch (Exception e) {
            throw new ForwardException(e);
        }
        System.err.println("== INIT SECURITY ===========================");
        throw new RuntimeException("Security.run() failed");
    }

    private byte[] decryptStoragePassword(byte[] input) {
        byte[] bytesPrivateKey = UtilBase64.decodeResultBytes(UtilByte.charsToBytes(privateKey));
        if (bytesPrivateKey == null || bytesPrivateKey.length == 0) {
            throw new RuntimeException("Private key is empty");
        }
        byte[] bytesPasswordKeyStore;
        try {
            bytesPasswordKeyStore = UtilRsa.decrypt(input, UtilRsa.getPrivateKey(bytesPrivateKey));
        } catch (Exception e) {
            UtilFile.removeIfExist(property.getPathPublicKey());
            new Thread(() -> {
                Util.testSleepMs(2_000);
                System.exit(-1);
            }).start();
            throw new ForwardException("Decrypt token exception. File: [" + property.getPathPublicKey() + "] removed, please restart application", e);
        }
        if (bytesPasswordKeyStore == null || bytesPasswordKeyStore.length == 0) {
            throw new RuntimeException("Decrypt Token empty. Change/remove token file: [" + property.getPathPublicKey() + "]");
        }
        return bytesPasswordKeyStore;
    }

    private byte[] getOrCreateJsonCred() {
        byte[] init;
        try {
            init = UtilFile.readBytes(property.getPathJsonCred());
        } catch (FileNotFoundException | NoSuchFileException exception) {
            //Если нет - создадим
            System.err.println("== NEED INIT SECURITY ===========================");
            System.err.println("** Update file [" + property.getPathJsonCred() + "]");
            System.err.println("== NEED INIT SECURITY ===========================");
            try {
                init = UtilFileResource.get("security.json").readAllBytes();
                UtilFile.writeBytes(property.getPathJsonCred(), init, FileWriteOptions.CREATE_OR_REPLACE);
            } catch (Exception e) {
                throw new ForwardException(e);
            }
            //Нет смысла продолжать работу, когда файл инициализации в данный момент пустой
            throw new RuntimeException("Update file [" + property.getPathJsonCred() + "]");
        } catch (IOException e) {
            //Если возникли другие проблемы при чтении файла инициализации прекратим работу
            throw new ForwardException(e);
        }
        return init;
    }

    private boolean checkPassword(char[] password) {
        try {
            return hashPassword != null
                    && hashPassword.equals(
                    new String(
                            Util.getHashByte(UtilByte.charsToBytes(password), hashPasswordType),
                            StandardCharsets.UTF_8
                    ));
        } catch (Exception e) {
            App.error(e);
        }
        return false;
    }

    public void loadKeyStorage(char[] password) throws Exception {
        if (password == null || password.length == 0) {
            throw new RuntimeException("Password is empty; Change/remove token file: [" + property.getPathPublicKey() + "]");
        }
        hashPassword = new String(Util.getHashByte(UtilByte.charsToBytes(password), hashPasswordType), StandardCharsets.UTF_8);
        keyStorePP = new KeyStore.PasswordProtection(password);
        File f = new File(property.getPathStorage());
        if (!f.exists()) {
            keyStore = KeyStore.getInstance(typeStorage);
            keyStore.load(null, password);
            save(password);
        } else {
            try (InputStream stream = new ByteArrayInputStream(UtilFile.readBytes(property.getPathStorage()))) {
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

    public char[] get(String alias) throws Exception {
        if (keyStore == null) {
            throw new RuntimeException("Security component not initialize");
        }
        KeyStore.SecretKeyEntry ske = (KeyStore.SecretKeyEntry) keyStore.getEntry(alias, keyStorePP);
        if (ske == null) {
            throw new RuntimeException("Alias: " + alias + " does not exist");
        }
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBE");
        PBEKeySpec keySpec = (PBEKeySpec) factory.getKeySpec(ske.getSecretKey(), PBEKeySpec.class);
        return keySpec.getPassword();
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
        UtilFile.writeBytes(property.getPathStorage(), byteArrayOutputStream.toByteArray(), FileWriteOptions.CREATE_OR_REPLACE);
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
                    UtilLog.printInfo(key);
                    try {
                        KeyStore.SecretKeyEntry ske = (KeyStore.SecretKeyEntry) finalKeyStore1.getEntry(key, keyStorePP);
                        if (ske != null) {
                            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBE");
                            PBEKeySpec keySpec = (PBEKeySpec) factory.getKeySpec(ske.getSecretKey(), PBEKeySpec.class);
                            UtilLog.printInfo(keySpec.getPassword());
                        }
                    } catch (Exception e) {
                        throw new ForwardException(e);
                    }
                });

            } catch (Exception e) {
                App.error(e);
            }
        } else {
            UtilLog.printInfo("file not exist");
        }
    }

    @Override
    public void runOperation() {
        propertyDispatcher.run();
        byte[] publicKey = UtilFile.readBytes(property.getPathPublicKey(), null);

        UtilLog.printInfo("Security Check privateKey: " + (privateKey != null && privateKey.length > 0)
                        + "\r\n"
                        + "Security Check publicKey: " + (publicKey != null && publicKey.length > 0));

        if (publicKey != null && publicKey.length > 0 && privateKey != null && privateKey.length > 0) {
            // У нас всё установлено можем просто работать
            byte[] passwordKeyStore = decryptStoragePassword(publicKey);
            if (passwordKeyStore.length == 0) {
                throw new RuntimeException("Decrypt password KeyStore is empty; Change/Remove [" + property.getPathPublicKey() + "]");
            }
            try {
                loadKeyStorage(UtilByte.bytesToChars(passwordKeyStore));
            } catch (Exception e) {
                throw new ForwardException(e);
            }
            if (UtilFile.ifExist(property.getPathJsonCred())) {
                UtilLog.printError("Please remove file [" + property.getPathJsonCred() + "] with credentials information");
                updateDataFromJsonCred(UtilByte.bytesToChars(passwordKeyStore));
            }
            try {
                UtilLog.info(getAvailableAliases())
                        .addHeader("description", "KeyStore available aliases")
                        .print();
            } catch (Exception ignore) {

            }
            setPrivateKey(new char[]{});
        } else {
            UtilFile.removeIfExist(property.getPathStorage());
            //У нас чего-то не хватает выводим предупреждения
            byte[] initJson = getOrCreateJsonCred();
            String passwordFromInfoJson = getPasswordFromJsonCred(initJson);
            printNotice(passwordFromInfoJson);
            System.exit(0);
        }
    }

    @Override
    public void shutdownOperation() {
        propertyDispatcher.shutdown();
    }

    @Override
    public int getInitializationIndex() {
        return 0;
    }

}