package ru.jamsys.virtual.file.system.view;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unused")
public class FileViewKeyStoreSslSocketFactory extends FileViewKeyStore {

    private final Map<String, SSLSocketFactory> sslSocketFactory = new ConcurrentHashMap<>();

    public SSLSocketFactory getSslSocketFactory(String sslContextType) {
        if (super.getKeyStore() == null) {
            return null;
        }
        if (!sslSocketFactory.containsKey(sslContextType)) {
            try {
                SSLContext ssl = SSLContext.getInstance(sslContextType);
                ssl.init(super.getKeyManagers(), super.getTrustManager().getListTrustManager(), new SecureRandom());
                sslSocketFactory.put(sslContextType, ssl.getSocketFactory());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sslSocketFactory.get(sslContextType);
    }

    @Override
    public void createCache() {
        super.createCache();
        sslSocketFactory.clear();
    }
}
