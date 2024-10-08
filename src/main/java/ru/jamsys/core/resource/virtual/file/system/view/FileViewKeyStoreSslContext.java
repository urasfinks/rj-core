package ru.jamsys.core.resource.virtual.file.system.view;

import ru.jamsys.core.extension.exception.ForwardException;

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
        return sslContext.computeIfAbsent(sslContextType, _ -> {
            try {
                SSLContext ssl = SSLContext.getInstance(sslContextType);
                ssl.init(super.getKeyManagers(), super.getTrustManager().getListTrustManager(), new SecureRandom());
                return ssl;
            } catch (Throwable th) {
                throw new ForwardException(th);
            }
        });
    }

    @Override
    public void createCache() {
        super.createCache();
        sslContext.clear();
    }
}
