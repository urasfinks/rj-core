package ru.jamsys.core.resource.virtual.file.system.view;

import lombok.Getter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.extension.exception.ForwardException;
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
        typeKeyStorage = file.getRepositoryMap(String.class, prop.TYPE.name(), "JCEKS");
        securityKey = file.getRepositoryMap(String.class, prop.SECURITY_KEY.name(), file.getFilePath().getPath());
        if (file.repositoryMapContains(prop.TRUST_MANAGER.name())) {
            trustManager = file.getRepositoryMap(CustomTrustManager.class, prop.TRUST_MANAGER.name());
        }
    }

    @Override
    public void createCache() {
        char[] pass = securityComponent.get(securityKey);
        try (InputStream stream = file.getInputStream()) {
            keyStore = KeyStore.getInstance(typeKeyStorage);
            keyStore.load(stream, pass);
        } catch (Throwable th) {
            keyStore = null;
            throw new ForwardException(th);
        }
        if (keyManagers == null) {
            try {
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(keyStore, pass);
                keyManagers = kmf.getKeyManagers();
            } catch (Throwable th) {
                throw new ForwardException(th);
            }
        }
    }

}
