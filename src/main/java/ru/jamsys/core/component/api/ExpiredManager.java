package ru.jamsys.core.component.api;

import org.springframework.stereotype.Component;
import ru.jamsys.core.component.AbstractManagerListItem;
import ru.jamsys.core.component.item.Expired;
import ru.jamsys.core.extension.KeepAliveComponent;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.time.immutable.ExpiredMsImmutableEnvelope;
import ru.jamsys.core.util.ControlExpiredKeepAliveResult;
import ru.jamsys.core.util.ExpiredManagerEnvelope;
import ru.jamsys.core.util.Util;
import ru.jamsys.core.util.UtilRisc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

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
        return null;
    }

}
