package ru.jamsys.core.component;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.ManagerBroker;
import ru.jamsys.core.component.manager.ManagerExpiration;
import ru.jamsys.core.component.manager.item.Expiration;
import ru.jamsys.core.extension.CascadeKey;
import ru.jamsys.core.extension.KeepAliveComponent;
import ru.jamsys.core.extension.StatisticsFlushComponent;
import ru.jamsys.core.extension.broker.persist.BrokerMemory;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseImpl;
import ru.jamsys.core.promise.PromiseTask;
import ru.jamsys.core.statistic.AvgMetric;
import ru.jamsys.core.statistic.Statistic;
import ru.jamsys.core.statistic.expiration.immutable.DisposableExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.expiration.immutable.ExpirationMsImmutableEnvelope;
import ru.jamsys.core.statistic.timer.nano.TimerNanoEnvelope;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

// Сервис управления перезапуска задач + контроль времени, сколько задачи исполняются

@Component
@Lazy
public class ServicePromise implements CascadeKey, KeepAliveComponent, StatisticsFlushComponent {

    public static Set<Promise> queueMultipleCompleteSet = Util.getConcurrentHashSet();

    BrokerMemory<Promise> broker;

    private final Expiration<PromiseTask> promiseTaskRetry;

    ConcurrentLinkedDeque<TimerNanoEnvelope<String>> queueTimer = new ConcurrentLinkedDeque<>();

    Map<String, LongSummaryStatistics> timeStatisticNano = new HashMap<>();

    private final ConcurrentLinkedDeque<Statistic> toStatistic = new ConcurrentLinkedDeque<>();

    public ServicePromise(ManagerBroker managerBroker, ManagerExpiration managerExpiration) {
        this.broker = managerBroker.initAndGet(
                getCascadeKey(),
                Promise.class,
                promise -> promise.timeOut(App.getUniqueClassName(getClass()) + ".onPromiseTaskExpired")
        );
        promiseTaskRetry = managerExpiration.get("PromiseTaskRetry", PromiseTask.class, this::onPromiseTaskRetry);
    }

    public Promise get(Class<?> cls, long timeOutMs) {
        return get(App.getUniqueClassName(cls), timeOutMs);
    }

    public Promise get(String index, long timeOutMs) {
        PromiseImpl promise = new PromiseImpl(index, timeOutMs);
        promise.setRegisterInBroker(broker.add(promise, timeOutMs));
        return promise;
    }

    public void finish(DisposableExpirationMsImmutableEnvelope<Promise> fin) {
        broker.remove(fin);
    }

    public void addRetryDelay(PromiseTask promiseTask) {
        promiseTaskRetry.add(new ExpirationMsImmutableEnvelope<>(promiseTask, promiseTask.getRetryDelayMs()));
    }

    public TimerNanoEnvelope<String> registrationTimer(String index) {
        TimerNanoEnvelope<String> timer = new TimerNanoEnvelope<>(index);
        queueTimer.add(timer);
        return timer;
    }

    private void onPromiseTaskRetry(DisposableExpirationMsImmutableEnvelope<PromiseTask> env) {
        PromiseTask promiseTask = env.getValue();
        if (promiseTask != null) {
            promiseTask.prepareLaunch(null);
        }
    }

    @Override
    public void keepAlive(AtomicBoolean threadRun) {
        Map<String, AvgMetric> mapMetricNano = new HashMap<>();
        UtilRisc.forEach(threadRun, queueTimer, (TimerNanoEnvelope<String> timer) -> {
            mapMetricNano.computeIfAbsent(timer.getValue(), _ -> new AvgMetric())
                    .add(timer.getOffsetLastActivityNano());
            if (timer.isStop()) {
                queueTimer.remove(timer);
            }
        });
        mapMetricNano.forEach((index, metric) -> {
            LongSummaryStatistics flush = metric.flush();
            timeStatisticNano.put(index, flush);
            toStatistic.add(new Statistic()
                    .addTag("index", index)
                    .addField("sum", flush.getSum())
            );
        });
    }

    @Override
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean threadRun) {
        List<Statistic> result = new ArrayList<>();
        while (!toStatistic.isEmpty()) {
            Statistic statistic = toStatistic.pollFirst();
            if (statistic != null) {
                statistic.addTags(parentTags).addFields(parentFields);
                result.add(statistic);
            }
        }
        return result;
    }

    @Override
    public String getKey() {
        return null;
    }

    @Override
    public CascadeKey getParentCascadeKey() {
        return App.cascadeName;
    }

}
