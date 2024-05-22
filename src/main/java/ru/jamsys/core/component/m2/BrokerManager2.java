package ru.jamsys.core.component.m2;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.item.Broker2;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

@Component
public class BrokerManager2 extends AbstractManager2<Broker2<?>>{

    private final ApplicationContext applicationContext;

    public BrokerManager2(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public Broker2<?> build(String index, Class<?> classItem) {
        return new Broker2<>(index, applicationContext, classItem);
    }

    public <MOI> DisposableExpirationMsImmutableEnvelope<MOI> add(String key, MOI element, long timeOut) throws Exception {
        Broker2<MOI> queue = get(key, element.getClass());
        return queue.add(new ExpirationMsImmutableEnvelope<>(element, timeOut));
    }

}
