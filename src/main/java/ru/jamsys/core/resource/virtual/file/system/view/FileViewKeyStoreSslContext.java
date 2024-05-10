package ru.jamsys.core.resource.virtual.file.system.view;

import ru.jamsys.core.App;
import ru.jamsys.core.component.ExceptionHandler;

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
            } catch (Exception e) {
                App.context.getBean(ExceptionHandler.class).handler(e);
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
