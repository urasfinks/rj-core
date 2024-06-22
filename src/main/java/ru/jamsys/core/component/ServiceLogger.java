package ru.jamsys.core.component;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.ManagerBroker;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.component.manager.item.Log;
import ru.jamsys.core.component.manager.item.LogType;
import ru.jamsys.core.extension.ClassNameImpl;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Lazy
public class ServiceLogger implements StatisticsFlushComponent {

    private Map<String, AtomicInteger> stat = new HashMap<>();

    Broker<Log> broker;

    public ServiceLogger(ManagerBroker managerBroker, ApplicationContext applicationContext) {
        broker = managerBroker.get(
                ClassNameImpl.getClassNameStatic(Log.class, null, applicationContext),
                Log.class
        );
        for (LogType type : LogType.values()) {
            stat.put(type.getName(), new AtomicInteger(0));
        }
    }

    public DisposableExpirationMsImmutableEnvelope<Log> add(Log log) {
        stat.get(log.logType.getName()).incrementAndGet();
        //return broker.add(new ExpirationMsImmutableEnvelope<>(log, 6_000));
        return null;
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isThreadRun) {
        List<Statistic> result = new ArrayList<>();
        Statistic statistic = new Statistic(parentTags, parentFields);
        for (String key : stat.keySet()) {
            statistic.addField(key, stat.get(key).getAndSet(0));
        }
        result.add(statistic);
        return result;
    }

}
