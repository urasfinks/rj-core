package ru.jamsys.core.resource.virtual.file.system.view;

import javax.net.ssl.SSLContext;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
public class FileViewKeyStoreSslContext extends FileViewKeyStore {

    private final Map<String, SSLContext> sslContext = new ConcurrentHashMap<>();

    public SSLContext getSslContext(String sslContextType) {
        if (super.getKeyStore() == null) {
            return null;
        }
        return sslContext.computeIfAbsent(sslContextType, s -> {
            try {
                SSLContext ssl = SSLContext.getInstance(sslContextType);
                ssl.init(super.getKeyManagers(), super.getTrustManager().getListTrustManager(), new SecureRandom());
                return ssl;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    @Override
    public void createCache() {
        super.createCache();
        sslContext.clear();
    }
}
