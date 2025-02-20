package ru.jamsys.core.component.manager;

import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.item.Expiration;
import ru.jamsys.core.component.manager.sub.AbstractManager;
import ru.jamsys.core.extension.CascadeName;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;

import java.util.function.Consumer;

//ExpirationManager.keepAlive() вызывает KeepAliveExpirationManager->Cron1s
@Component
public class ManagerExpiration extends
        AbstractManager<Expiration<?>,
                Consumer<DisposableExpirationMsImmutableEnvelope<?>>>
        implements CascadeName {

    @SuppressWarnings("all")
    public <T> Expiration<T> get(
            String index,
            Class<T> classItem,
            Consumer<DisposableExpirationMsImmutableEnvelope<T>> onExpired
    ) {
        Consumer<DisposableExpirationMsImmutableEnvelope<?>> xx = (Consumer) onExpired;
        return (Expiration) getManagerElement(index, classItem, xx);
    }

    @Override
    public Expiration<?> build(
            String key,
            Class<?> classItem,
            Consumer<DisposableExpirationMsImmutableEnvelope<?>> builderArgument
    ) {
        return new Expiration<>(this, key, classItem, builderArgument);
    }

    @Override
    public int getInitializationIndex() {
        return 4;
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
