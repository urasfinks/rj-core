package ru.jamsys.core.component;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.cron.LogUploader;
import ru.jamsys.core.component.manager.ManagerBroker;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.component.manager.item.Log;
import ru.jamsys.core.component.manager.item.LogType;
import ru.jamsys.core.extension.ClassNameImpl;
import ru.jamsys.core.extension.LifeCycleComponent;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.extension.property.PropertyConnector;
import ru.jamsys.core.extension.property.PropertyName;
import ru.jamsys.core.extension.property.PropertySubscriberNotify;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Lazy
public class ServiceLogger extends PropertyConnector implements
        StatisticsFlushComponent,
        LifeCycleComponent {

    private final Map<String, AtomicInteger> stat = new HashMap<>();

    Broker<Log> broker;

    @PropertyName("remote.log")
    private String remoteLog = "true";

    public ServiceLogger(ManagerBroker managerBroker, ApplicationContext applicationContext) {
        broker = managerBroker.get(
                ClassNameImpl.getClassNameStatic(Log.class, null, applicationContext),
                Log.class
        );
        for (LogType type : LogType.values()) {
            stat.put(type.getName(), new AtomicInteger(0));
        }
        applicationContext
                .getBean(ServiceProperty.class)
                .getSubscriber(null, this, "run.args");
    }

    public DisposableExpirationMsImmutableEnvelope<Log> add(Log log) {
        stat.get(log.logType.getName()).incrementAndGet();
        if (remoteLog.equals("true")) {
            return broker.add(new ExpirationMsImmutableEnvelope<>(log, 6_000));
        }
        return null;
    }

    public DisposableExpirationMsImmutableEnvelope<Log> add(
            LogType logType,
            Map<String, Object> data,
            String extIndex,
            boolean print
    ) {
        String stringData = UtilJson.toStringPretty(data, "{}");
        Log log = new Log(logType).setData(stringData).setExtIndex(extIndex);
        if (print) {
            switch (logType) {
                case ERROR, SYSTEM_EXCEPTION -> Util.logConsole("::" + extIndex + "; " + stringData, true);
                case INFO, DEBUG -> Util.logConsole("::" + extIndex + "; " + stringData, false);
            }
        }
        return add(log);
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

    @Override
    public int getInitializationIndex() {
        return 999;
    }

    @Override
    public void run() {

    }

    @Override
    public void shutdown() {
        if (remoteLog.equals("true") && !broker.isEmpty()) {
            App.get(LogUploader.class).generate().run().await(5000);
        }
    }

}
