package ru.jamsys.core.component;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.LifeCycleComponent;
import ru.jamsys.core.extension.annotation.PropertyName;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.property.PropertiesAgent;
import ru.jamsys.core.extension.property.repository.RepositoryPropertiesField;
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
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class SecurityComponent extends RepositoryPropertiesField implements LifeCycleComponent {

    @Setter
    @PropertyName("run.args.security.path.storage")
    private String pathStorage;

    @Setter
    @PropertyName("run.args.security.path.public.key")
    private String pathPublicKey;

    @Setter
    @PropertyName("run.args.security.path.init")
    private String pathJsonCred;

    @Getter
    @SuppressWarnings("all")
    @PropertyName("run.args.security.path.java")
    private String pathInitSecurityKeyJava;

    final private ExceptionHandler exceptionHandler;

    private volatile KeyStore keyStore = null;

    private char[] privateKey = {};

    private String hashPassword = null;

    private final String hashPasswordType = "SHA1";

    public String typeStorage = "JCEKS";

    private KeyStore.PasswordProtection keyStorePP;

    private final PropertiesAgent propertiesAgent;

    private final AtomicBoolean isRun = new AtomicBoolean(false);

    public SecurityComponent(ServiceProperty serviceProperty, ExceptionHandler exceptionHandler) {
        propertiesAgent = serviceProperty.getFactory().getPropertiesAgent(
                null,
                this,
                null,
                true
        );
        this.exceptionHandler = exceptionHandler;
    }

    @SuppressWarnings("unused")
    public void setPrivateKey(char[] newPrivateKey) {
        privateKey = newPrivateKey;
    }

    private void updateDataFromJsonCred(char[] password) {
        try {
            byte[] initJson = UtilFile.readBytes(pathJsonCred);
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

    private String getPasswordFromJsonCred(byte[] initJson) {
        if (initJson == null || initJson.length == 0) {
            throw new RuntimeException("File: [" + pathJsonCred + "] is empty");
        }
        String result = null;
        String initString = new String(initJson, StandardCharsets.UTF_8);
        JsonEnvelope<Map<String, Object>> mapJsonEnvelope = UtilJson.toMap(initString);
        if (mapJsonEnvelope.getException() == null && !mapJsonEnvelope.getObject().isEmpty()) {
            result = (String) mapJsonEnvelope.getObject().get("password");
        }
        if (result == null || result.trim().isEmpty()) {
            throw new RuntimeException("Password json field from [" + pathJsonCred + "] is empty");
        }
        return result;
    }

    private void printNotice(String password) {
        try {
            if (password != null && !password.trim().isEmpty()) {
                KeyPair keyPair = UtilRsa.genKeyPair();
                byte[] token = UtilRsa.encrypt(password.getBytes(StandardCharsets.UTF_8), keyPair);
                UtilFile.writeBytes(pathPublicKey, token, FileWriteOptions.CREATE_OR_REPLACE);
                String privateKey = UtilBase64.encode(keyPair.getPrivate().getEncoded(), true);
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
                System.err.println("** Update file [" + pathJsonCred + "]; password field must not be empty");
            }
        } catch (Exception e) {
            throw new ForwardException("Other problem", e);
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
            UtilFile.removeIfExist(pathPublicKey);
            throw new ForwardException("Decrypt token exception. File: [" + pathPublicKey + "] removed, please restart application", e);
        }
        if (bytesPasswordKeyStore == null || bytesPasswordKeyStore.length == 0) {
            throw new RuntimeException("Decrypt Token empty. Change/remove token file: [" + pathPublicKey + "]");
        }
        return bytesPasswordKeyStore;
    }

    private byte[] getOrCreateJsonCred() {
        byte[] init;
        try {
            init = UtilFile.readBytes(pathJsonCred);
        } catch (FileNotFoundException | NoSuchFileException exception) {
            //Если нет - создадим
            System.err.println("== NEED INIT SECURITY ===========================");
            System.err.println("** Update file [" + pathJsonCred + "]");
            System.err.println("== NEED INIT SECURITY ===========================");
            try {
                init = UtilFileResource.get("security.json").readAllBytes();
                UtilFile.writeBytes(pathJsonCred, init, FileWriteOptions.CREATE_OR_REPLACE);
            } catch (Exception e) {
                throw new ForwardException(e);
            }
            //Нет смысла продолжать работу, когда файл инициализации в данный момент пустой
            throw new RuntimeException("Update file [" + pathJsonCred + "]");
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
            exceptionHandler.handler(e);
        }
        return false;
    }

    public void loadKeyStorage(char[] password) throws Exception {
        if (password == null || password.length == 0) {
            throw new RuntimeException("Password is empty; Change/remove token file: [" + pathPublicKey + "]");
        }
        hashPassword = new String(Util.getHashByte(UtilByte.charsToBytes(password), hashPasswordType), StandardCharsets.UTF_8);
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

    public char[] get(String key) {
        try {
            if (keyStore != null) {
                KeyStore.SecretKeyEntry ske = (KeyStore.SecretKeyEntry) keyStore.getEntry(key, keyStorePP);
                if (ske != null) {
                    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBE");
                    PBEKeySpec keySpec = (PBEKeySpec) factory.getKeySpec(ske.getSecretKey(), PBEKeySpec.class);
                    return keySpec.getPassword();
                }
                return null;
            } else {
                throw new RuntimeException("Security компонент не инициализирован");
            }
        } catch (Exception e) {
            throw new ForwardException(e);
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
                        throw new ForwardException(e);
                    }
                });

            } catch (Exception e) {
                App.error(e);
            }
        } else {
            Util.logConsole("file not exist");
        }
    }

    @Override
    public void run() {
        if (isRun.compareAndSet(false, true)) {
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
                    throw new ForwardException(e);
                }
                if (UtilFile.ifExist(pathJsonCred)) {
                    Util.logConsole("Please remove file [" + pathJsonCred + "] with credentials information", true);
                    updateDataFromJsonCred(UtilByte.bytesToChars(passwordKeyStore));
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
                byte[] initJson = getOrCreateJsonCred();
                String passwordFromInfoJson = getPasswordFromJsonCred(initJson);
                printNotice(passwordFromInfoJson);
            }
            propertiesAgent.run();
        }
    }

    @Override
    public void shutdown() {
        propertiesAgent.shutdown();
    }

    @Override
    public int getInitializationIndex() {
        return 0;
    }

}