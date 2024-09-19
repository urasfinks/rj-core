package ru.jamsys.core.component;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.core.component.manager.ManagerBroker;
import ru.jamsys.core.component.manager.ManagerExpiration;
import ru.jamsys.core.component.manager.item.Broker;
import ru.jamsys.core.component.manager.item.Expiration;
import ru.jamsys.core.extension.UniqueClassName;
import ru.jamsys.core.extension.KeepAliveComponent;
import ru.jamsys.core.extension.StatisticsFlushComponent;
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

@Component
@Lazy
public class ServicePromise implements UniqueClassName, KeepAliveComponent, StatisticsFlushComponent {

    public static Set<Promise> queueMultipleCompleteSet = Util.getConcurrentHashSet();

    Broker<Promise> broker;

    private final Expiration<PromiseTask> promiseTaskRetry;

    ConcurrentLinkedDeque<TimerNanoEnvelope<String>> queueTimer = new ConcurrentLinkedDeque<>();

    Map<String, LongSummaryStatistics> timeStatisticNano = new HashMap<>();

    private final ConcurrentLinkedDeque<Statistic> toStatistic = new ConcurrentLinkedDeque<>();

    public ServicePromise(ManagerBroker managerBroker, ApplicationContext applicationContext, ManagerExpiration managerExpiration) {
        this.broker = managerBroker.initAndGet(
                getClassName("run", applicationContext),
                Promise.class,
                promise -> promise.timeOut(getClassName("onPromiseTaskExpired"))
        );
        promiseTaskRetry = managerExpiration.get("PromiseTaskRetry", PromiseTask.class, this::onPromiseTaskRetry);
    }

    public Promise get(String index, long timeout) {
        PromiseImpl promise = new PromiseImpl(index, timeout);
        promise.setRegisterInBroker(broker.add(promise, timeout));
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
    public void keepAlive(AtomicBoolean isThreadRun) {
        Map<String, AvgMetric> mapMetricNano = new HashMap<>();
        UtilRisc.forEach(isThreadRun, queueTimer, (TimerNanoEnvelope<String> timer) -> {
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
    public List<Statistic> flushAndGetStatistic(Map<String, String> parentTags, Map<String, Object> parentFields, AtomicBoolean isThreadRun) {
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

}
