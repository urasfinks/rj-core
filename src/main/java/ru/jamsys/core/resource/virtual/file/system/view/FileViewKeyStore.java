package ru.jamsys.core.resource.virtual.file.system.view;

import lombok.Getter;

import ru.jamsys.core.App;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.component.Security;
import ru.jamsys.core.resource.virtual.file.system.File;
import ru.jamsys.core.resource.virtual.file.system.view.KeyStore.CustomTrustManager;

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
        typeKeyStorage = file.getProperty(prop.TYPE.name(), String.class, "JCEKS");
        securityKey = file.getProperty(prop.SECURITY_KEY.name(), String.class, file.getAbsolutePath());
        if (file.isProperty(prop.TRUST_MANAGER.name())) {
            trustManager = file.getProperty(prop.TRUST_MANAGER.name(), CustomTrustManager.class, null);
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
                App.context.getBean(ExceptionHandler.class).handler(e);
            }
            if (keyManagers == null) {
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(keyStore, pass);
                keyManagers = kmf.getKeyManagers();
            }
        } catch (Exception e) {
            App.context.getBean(ExceptionHandler.class).handler(e);
        }
    }

}
