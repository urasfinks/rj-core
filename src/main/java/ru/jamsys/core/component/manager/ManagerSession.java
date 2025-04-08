package ru.jamsys.core.component.manager;

import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.item.Session;
import ru.jamsys.core.component.manager.sub.AbstractManager;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.KeepAliveComponent;

@Component
public class ManagerSession extends AbstractManager<Session, Integer>
        implements KeepAliveComponent, CascadeKey {

    public <K, V> Session<K, V> get(String index, Integer keepAliveOnInactivityMs) {
        @SuppressWarnings("all")
        Session<K, V> session = getManagerElement(index, Integer.class, keepAliveOnInactivityMs);
        return session;
    }

    @Override
    public Session build(String key, Class<?> classItem, Integer builderArgument) {
        return new Session(getCascadeKey(key, classItem), builderArgument);
    }

    @Override
    public int getInitializationIndex() {
        return 502;
    }

    @Override
    public String getKey() {
        return null;
    }

    @Override
    public CascadeKey getParentCascadeKey() {
        return App.cascadeName;
    }

}
