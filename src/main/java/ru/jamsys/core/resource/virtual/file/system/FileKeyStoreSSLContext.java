package ru.jamsys.core.resource.virtual.file.system;

import lombok.Getter;
import ru.jamsys.core.extension.CustomTrustManager;
import ru.jamsys.core.extension.exception.ForwardException;

import javax.net.ssl.SSLContext;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class FileKeyStoreSSLContext extends FileKeyStore {

    CustomTrustManager trustManager = new CustomTrustManager();

    public FileKeyStoreSSLContext(String path) {
        super(path);
    }

    public void setupTrustManager(CustomTrustManager trustManager){
        this.trustManager = trustManager;
    }

    private final Map<String, SSLContext> cache = new ConcurrentHashMap<>();

    public SSLContext get(String protocol) {
        return cache.computeIfAbsent(protocol, _ -> {
            try {
                SSLContext ssl = SSLContext.getInstance(protocol);
                ssl.init(super.getKeyManagers(), trustManager.getListTrustManager(), new SecureRandom());
                return ssl;
            } catch (Throwable th) {
                throw new ForwardException(th);
            }
        });
    }

}
