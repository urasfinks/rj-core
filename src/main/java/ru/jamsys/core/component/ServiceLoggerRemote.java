package ru.jamsys.core.component;

import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.component.manager.item.Log;
import ru.jamsys.core.component.manager.item.LogType;
import ru.jamsys.core.extension.CascadeName;
import ru.jamsys.core.extension.LifeCycleComponent;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.extension.annotation.PropertyKey;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.repository.AnnotationPropertyExtractor;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

// Сервис просто пропускает через себя логи и ведёт статистику сколько по типу через него проходит

@FieldNameConstants
@Component
@Lazy
public class ServiceLoggerRemote extends AnnotationPropertyExtractor<Boolean> implements
        StatisticsFlushComponent,
        LifeCycleComponent,
        CascadeName {

    private final AtomicBoolean run = new AtomicBoolean(false);

    private final Map<String, AtomicInteger> stat = new HashMap<>();

    private final Broker<Log> broker;

    @SuppressWarnings("all")
    @Getter
    @PropertyKey("remote")
    private Boolean remote = false;

    final PropertyDispatcher<Boolean> propertyDispatcher;

    public ServiceLoggerRemote(ApplicationContext applicationContext) {
        broker = applicationContext.getBean(Core.class).getLogBroker();
        for (LogType type : LogType.values()) {
            stat.put(type.getNameCamel(), new AtomicInteger(0));
        }
        propertyDispatcher = new PropertyDispatcher<>(
                applicationContext.getBean(ServiceProperty.class),
                null,
                this,
                "log.uploader"
        );
    }

    public DisposableExpirationMsImmutableEnvelope<Log> add(Log log) {
        stat.get(log.getLogType().getNameCamel()).incrementAndGet();
        if (remote) {
            return broker.add(new ExpirationMsImmutableEnvelope<>(log, 6_000));
        }
        return null;
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
        propertyDispatcher.run();
        run.set(true);
    }

    @Override
    public void shutdown() {
        propertyDispatcher.shutdown();
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
