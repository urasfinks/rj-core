package ru.jamsys.core.resource.virtual.file.system.view;

import ru.jamsys.core.App;
import ru.jamsys.core.component.ExceptionHandler;

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
        return sslSocketFactory.computeIfAbsent(sslContextType, _ -> {
            try {
                SSLContext ssl = SSLContext.getInstance(sslContextType);
                ssl.init(super.getKeyManagers(), super.getTrustManager().getListTrustManager(), new SecureRandom());
                return ssl.getSocketFactory();
            } catch (Exception e) {
                App.context.getBean(ExceptionHandler.class).handler(e);
            }
            return null;
        });
    }

    @Override
    public void createCache() {
        super.createCache();
        sslSocketFactory.clear();
    }
}
