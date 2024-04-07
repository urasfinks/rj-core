package ru.jamsys.virtual.file.system.view;

import lombok.Getter;
import ru.jamsys.App;
import ru.jamsys.component.Security;
import ru.jamsys.virtual.file.system.File;
import ru.jamsys.virtual.file.system.view.KeyStore.CustomTrustManager;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;


public class FileViewKeyStore implements FileView {

    public enum prop {
        TYPE,
        SECURITY_KEY,
        TRUST_MANAGER
    }

    private String typeKeyStorage;
    private String securityKey;
    private Security security;
    private File file = null;

    @Getter
    private KeyManager[] keyManagers;

    @Getter
    CustomTrustManager trustManager = new CustomTrustManager();

    @Getter
    private volatile KeyStore keyStore = null;

    @Override
    public void set(File file) {
        this.file = file;
        security = App.context.getBean(Security.class);
        typeKeyStorage = file.getProp(prop.TYPE.name(), "JCEKS");
        securityKey = file.getProp(prop.SECURITY_KEY.name(), file.getAbsolutePath());
        if (file.isProp(prop.TRUST_MANAGER.name())) {
            trustManager = file.getProp(prop.TRUST_MANAGER.name(), null);
        }
    }

    @Override
    public void createCache() {
        try {
            char[] pass = security.get(securityKey);
            try (InputStream stream = file.getInputStream()) {
                keyStore = KeyStore.getInstance(typeKeyStorage);
                keyStore.load(stream, pass);
            } catch (Exception e) {
                keyStore = null;
                e.printStackTrace();
            }
            if (keyManagers == null) {
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(keyStore, pass);
                keyManagers = kmf.getKeyManagers();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
