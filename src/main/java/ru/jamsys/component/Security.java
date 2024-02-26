package ru.jamsys.component;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.AbstractCoreComponent;
import ru.jamsys.FileWriteOptions;
import ru.jamsys.UtilFile;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.*;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Lazy
public class Security extends AbstractCoreComponent {

    @Setter
    @Value("${rj.core.security.path:security/security.jks}")
    private String path;

    private volatile KeyStore keyStore = null;
    private char[] password;
    AtomicBoolean isInit = new AtomicBoolean(false);

    @Override
    public void run() {
        super.run();
    }

    public void init(char[] password) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        if (isInit.compareAndSet(false, true)) {
            this.password = password;

            File f = new File(path);
            if (!f.exists()) {
                createKeyStore();
            } else {
                try (InputStream stream = new ByteArrayInputStream(UtilFile.readBytes(path))) {
                    keyStore = KeyStore.getInstance("JCEKS");
                    keyStore.load(stream, this.password);
                } catch (Exception e) {
                    keyStore = null;
                    e.printStackTrace();
                }
            }
        }
    }

    private void createKeyStore() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        keyStore = KeyStore.getInstance("JCEKS");
        keyStore.load(null, password);
        save();
    }

    public void add(String key, char[] value) {
        if (keyStore != null) {
            try {
                KeyStore.PasswordProtection keyStorePP = new KeyStore.PasswordProtection(password);
                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBE");
                SecretKey generatedSecret = factory.generateSecret(new PBEKeySpec(value, "any".getBytes(), 13));
                keyStore.setEntry(key, new KeyStore.SecretKeyEntry(generatedSecret), keyStorePP);
                save();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            new Exception("Security компонент не инициализирован, исполните init(${pass})").printStackTrace();
        }
    }

    @SuppressWarnings("unused")
    public boolean isInit() {
        return keyStore != null;
    }

    public char[] get(String key) throws Exception {
        if (keyStore != null) {
            KeyStore.PasswordProtection keyStorePP = new KeyStore.PasswordProtection(password);
            KeyStore.SecretKeyEntry ske = (KeyStore.SecretKeyEntry) keyStore.getEntry(key, keyStorePP);
            if (ske != null) {
                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBE");
                PBEKeySpec keySpec = (PBEKeySpec) factory.getKeySpec(ske.getSecretKey(), PBEKeySpec.class);
                return keySpec.getPassword();
            }
            return null;
        } else {
            throw new Exception("Security компонент не инициализирован, исполните init(${pass})");
        }
    }

    public void remove(String key) {
        try {
            keyStore.deleteEntry(key);
            save();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void save() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        keyStore.store(byteArrayOutputStream, password);
        UtilFile.writeBytes(path, byteArrayOutputStream.toByteArray(), FileWriteOptions.CREATE_OR_REPLACE);
    }


    @Override
    public void flushStatistic() {

    }
}