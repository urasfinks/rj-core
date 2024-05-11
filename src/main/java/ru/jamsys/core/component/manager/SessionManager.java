package ru.jamsys.core.component.manager;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.item.Session;
import ru.jamsys.core.extension.KeepAliveComponent;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableEnvelope;

// MO - MapObject
// MOI - MapObjectItem

@SuppressWarnings({"unused", "UnusedReturnValue"})
@Component
@Lazy
public class SessionManager<MOI>
        extends
        AbstractManagerMapItem<
                Session<String, MOI>,
                ExpirationMsMutableEnvelope<MOI>
                >
        implements
        KeepAliveComponent,
        StatisticsFlushComponent {

    @Override
    public Session<String, MOI> build(String index) {
        return new Session<>(index);
    }
}
