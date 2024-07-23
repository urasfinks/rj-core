package ru.jamsys.core.resource.virtual.file.system.view;

import lombok.Getter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
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
    private SecurityComponent securityComponent;
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
        securityComponent = App.get(SecurityComponent.class);
        typeKeyStorage = file.getFromMapRepository(prop.TYPE.name(), String.class, "JCEKS");
        securityKey = file.getFromMapRepository(prop.SECURITY_KEY.name(), String.class, file.getAbsolutePath());
        if (file.mapRepositoryContains(prop.TRUST_MANAGER.name())) {
            trustManager = file.getFromMapRepository(prop.TRUST_MANAGER.name(), CustomTrustManager.class, null);
        }
    }

    @Override
    public void createCache() {
        try {
            char[] pass = securityComponent.get(securityKey);
            try (InputStream stream = file.getInputStream()) {
                keyStore = KeyStore.getInstance(typeKeyStorage);
                keyStore.load(stream, pass);
            } catch (Exception e) {
                keyStore = null;
                App.error(e);
            }
            if (keyManagers == null) {
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(keyStore, pass);
                keyManagers = kmf.getKeyManagers();
            }
        } catch (Exception e) {
            App.error(e);
        }
    }

}
