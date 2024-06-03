package ru.jamsys.core.component.manager;

import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.item.Expiration;
import ru.jamsys.core.component.manager.sub.AbstractManager;
import ru.jamsys.core.component.manager.sub.ManagerElement;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;

import java.util.function.Consumer;

//ExpirationManager.keepAlive() вызывает KeepAliveExpirationManager->Cron1s
@Component
public class ExpirationManager extends AbstractManager<Expiration<?>, Consumer<DisposableExpirationMsImmutableEnvelope<?>>> {

    @SuppressWarnings("all")
    public <T> ManagerElement<
                Expiration<T>,
                Consumer<DisposableExpirationMsImmutableEnvelope<T>>
                > get(
            String index,
            Class<T> classItem,
            Consumer<DisposableExpirationMsImmutableEnvelope<T>> onExpired
    ) {
        AbstractManager<?, Consumer<DisposableExpirationMsImmutableEnvelope<T>>> x = (AbstractManager) this;
        return new ManagerElement<>(index, classItem, x, onExpired);
    }

    @Override
    public Expiration<?> build(
            String index,
            Class<?> classItem,
            Consumer<DisposableExpirationMsImmutableEnvelope<?>> builderArgument
    ) {
        return new Expiration<>(index, classItem, builderArgument);
    }

}
