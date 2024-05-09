package ru.jamsys.core.component.api;

import org.springframework.stereotype.Component;
import ru.jamsys.core.component.AbstractManagerListItem;
import ru.jamsys.core.component.item.Expired;
import ru.jamsys.core.extension.KeepAliveComponent;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.statistic.time.immutable.ExpiredMsImmutableEnvelope;


@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
public class ExpiredManager<V>
        extends AbstractManagerListItem<
        Expired<V>,
        ExpiredMsImmutableEnvelope<V>,
        ExpiredMsImmutableEnvelope<V>
        > implements StatisticsFlushComponent, KeepAliveComponent{


    @Override
    public Expired<V> build(String index) {
        return new Expired<>(index);
    }

}
