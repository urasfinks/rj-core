package ru.jamsys.core.component.manager;

import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.item.Session;
import ru.jamsys.core.component.manager.sub.AbstractManager;
import ru.jamsys.core.extension.CascadeName;
import ru.jamsys.core.extension.KeepAliveComponent;

@Component
public class ManagerSession extends AbstractManager<Session, Integer>
        implements KeepAliveComponent, CascadeName {

    public <K, V> Session<K, V> get(String index, Integer keepAliveOnInactivityMs) {
        @SuppressWarnings("all")
        Session<K, V> session = getManagerElement(index, Integer.class, keepAliveOnInactivityMs);
        return session;
    }

    @Override
    public Session build(String key, Class<?> classItem, Integer builderArgument) {
        return new Session(this, key, builderArgument);
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
    public CascadeName getParentCascadeName() {
        return App.cascadeName;
    }

}
