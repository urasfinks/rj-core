package ru.jamsys.core.component.api;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.AbstractManagerMapItem;
import ru.jamsys.core.component.item.Session;
import ru.jamsys.core.extension.KeepAliveComponent;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.statistic.time.mutable.ExpiredMsMutableEnvelope;

// MO - MapObject
// MOI - MapObjectItem

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
@Lazy
public class SessionManager<MOI>
        extends
        AbstractManagerMapItem<
                Session<String, MOI>,
                ExpiredMsMutableEnvelope<MOI>
                >
        implements
        KeepAliveComponent,
        StatisticsFlushComponent {

    @Override
    public Session<String, MOI> build(String index) {
        return new Session<>(index);
    }
}
