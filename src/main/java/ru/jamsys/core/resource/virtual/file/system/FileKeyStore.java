package ru.jamsys.core.resource.virtual.file.system;

import lombok.Getter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.flat.util.UtilByte;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;

@Getter
public class FileKeyStore extends File {

    private String securityAlias;

    private String typeKeyStorage = "JCEKS";

    private String algorithmKeyManager = KeyManagerFactory.getDefaultAlgorithm();

    private volatile KeyStore keyStore = null;

    private KeyManager[] keyManagers;

    public FileKeyStore(String path) {
        super(path);
    }

    public void setupTypeKeyStorage(String typeKeyStorage) {
        this.typeKeyStorage = typeKeyStorage;
    }

    public void setupSecurityAlias(String securityAlias) {
        this.securityAlias = securityAlias;
    }

    @SuppressWarnings("unused")
    public void setupAlgorithmKeyManager(String algorithmKeyManager) {
        this.algorithmKeyManager = algorithmKeyManager;
    }

    @Override
    public void reloadBytes() {
        try {
            bytes = getReadFromSource().get();
            char[] pass = App.get(SecurityComponent.class).get(securityAlias);
            try (InputStream stream = UtilByte.getInputStream(bytes)) {
                keyStore = KeyStore.getInstance(typeKeyStorage);
                keyStore.load(stream, pass);
            } catch (Throwable th) {
                keyStore = null;
                throw new ForwardException(this, th);
            }
            try {
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithmKeyManager);
                kmf.init(keyStore, pass);
                keyManagers = kmf.getKeyManagers();
            } catch (Throwable th) {
                throw new ForwardException(this, th);
            }
        } catch (Throwable th) {
            throw new ForwardException(this, th);
        }
    }

}
