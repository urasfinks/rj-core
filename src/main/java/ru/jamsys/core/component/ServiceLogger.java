package ru.jamsys.core.component;

import lombok.Getter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.cron.LogUploader;
import ru.jamsys.core.component.manager.ManagerBroker;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.component.manager.item.Log;
import ru.jamsys.core.component.manager.item.LogType;
import ru.jamsys.core.extension.CascadeName;
import ru.jamsys.core.extension.LifeCycleComponent;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.extension.annotation.PropertyName;
import ru.jamsys.core.extension.property.PropertySubscriber;
import ru.jamsys.core.extension.property.repository.AnnotationPropertyExtractor;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Lazy
public class ServiceLogger extends AnnotationPropertyExtractor implements
        StatisticsFlushComponent,
        LifeCycleComponent,
        CascadeName {

    private final AtomicBoolean run = new AtomicBoolean(false);

    private final Map<String, AtomicInteger> stat = new HashMap<>();

    Broker<Log> broker;

    @SuppressWarnings("all")
    @Getter
    @PropertyName("remote")
    private Boolean remote = false;

    public ServiceLogger(ManagerBroker managerBroker, ApplicationContext applicationContext) {
        broker = managerBroker.get(
                getCascadeName(App.getUniqueClassName(Log.class)),
                Log.class
        );
        for (LogType type : LogType.values()) {
            stat.put(type.getNameCamel(), new AtomicInteger(0));
        }
        new PropertySubscriber(
                applicationContext.getBean(ServiceProperty.class),
                null,
                this,
                "log.uploader"
        ); //Без run() просто заполнить значения
    }

    public DisposableExpirationMsImmutableEnvelope<Log> add(Log log) {
        stat.get(log.logType.getNameCamel()).incrementAndGet();
        if (remote) {
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
                case ERROR, SYSTEM_EXCEPTION -> Util.logConsole(getClass(), "::" + extIndex + "; " + stringData, true);
                case INFO, DEBUG -> Util.logConsole(getClass(), "::" + extIndex + "; " + stringData, false);
            }
        }
        return add(log);
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean threadRun) {
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
    public boolean isRun() {
        return run.get();
    }

    @Override
    public void run() {
        run.set(true);
    }

    @Override
    public void shutdown() {
        if (remote && !broker.isEmpty()) {
            Promise promise = App.get(LogUploader.class).generate();
            if (promise != null) {
                promise.run().await(5000);
            }
        }
        run.set(false);
    }

    @Override
    public String getKey() {
        return null;
    }

    @Override
    public CascadeName getParentCascadeName() {
        return App.cascadeName;
    }
}
