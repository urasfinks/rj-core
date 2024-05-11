package ru.jamsys.core.component.manager;

import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.item.Expiration;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;


@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
public class ExpirationManager<V>
        extends AbstractManagerListItem<
        Expiration<V>,
        ExpirationMsImmutableEnvelope<V>,
        DisposableExpirationMsImmutableEnvelope<V>
        > implements StatisticsFlushComponent{

    public ExpirationManager() {
        setCleanableMap(false);
    }

    @Override
    public Expiration<V> build(String index) {
        return new Expiration<>(index);
    }

}
