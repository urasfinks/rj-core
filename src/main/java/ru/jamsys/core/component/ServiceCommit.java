package ru.jamsys.core.component;

import lombok.experimental.FieldNameConstants;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.CascadeName;
import ru.jamsys.core.extension.LifeCycleComponent;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.statistic.Statistic;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

// Задумалось создать сервис задач, по которым надо отчитываться (делать коммит, что задача исполнена)

@FieldNameConstants
@Component
@Lazy
public class ServiceCommit implements
        StatisticsFlushComponent,
        LifeCycleComponent,
        CascadeName {

    private final AtomicBoolean run = new AtomicBoolean(false);

    public void commit() {

    }

    public void rollback() {

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

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean threadRun) {
        return List.of();
    }

}
