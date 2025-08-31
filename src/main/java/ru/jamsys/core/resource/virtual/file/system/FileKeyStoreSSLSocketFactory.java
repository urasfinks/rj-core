package ru.jamsys.core.resource.virtual.file.system;

import lombok.Getter;
import ru.jamsys.core.extension.CustomTrustManager;
import ru.jamsys.core.extension.exception.ForwardException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class FileKeyStoreSSLSocketFactory extends FileKeyStore {

    CustomTrustManager trustManager = new CustomTrustManager();

    public FileKeyStoreSSLSocketFactory(String ns, String key) {
        super(ns, key);
    }

    @SuppressWarnings("unused")
    public void setupTrustManager(CustomTrustManager trustManager){
        this.trustManager = trustManager;
    }

    private final Map<String, SSLSocketFactory> cache = new ConcurrentHashMap<>();

    public SSLSocketFactory get(String protocol) {
        return cache.computeIfAbsent(protocol, _ -> {
            try {
                SSLContext ssl = SSLContext.getInstance(protocol);
                ssl.init(super.getKeyManagers(), trustManager.getListTrustManager(), new SecureRandom());
                return ssl.getSocketFactory();
            } catch (Throwable th) {
                throw new ForwardException(this, th);
            }
        });
    }

}
