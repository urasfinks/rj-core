package ru.jamsys.core.component.api;

import org.springframework.stereotype.Component;
import ru.jamsys.core.component.AbstractManagerListItem;
import ru.jamsys.core.component.item.Expiration;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.statistic.time.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.time.immutable.ExpirationMsImmutableEnvelope;


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
