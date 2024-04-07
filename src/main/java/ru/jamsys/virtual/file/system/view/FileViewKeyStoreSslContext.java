package ru.jamsys.virtual.file.system.view;

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
        if (!sslContext.containsKey(sslContextType)) {
            try {
                SSLContext ssl = SSLContext.getInstance(sslContextType);
                ssl.init(super.getKeyManagers(), super.getTrustManager().getListTrustManager(), new SecureRandom());
                sslContext.put(sslContextType, ssl);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sslContext.get(sslContextType);
    }

    @Override
    public void createCache() {
        super.createCache();
        sslContext.clear();
    }
}
